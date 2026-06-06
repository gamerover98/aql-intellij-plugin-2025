package com.arangodb.intellij.aql.spring

import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.ui.console.AqlConsoleVirtualFile
import com.arangodb.intellij.aql.ui.console.AqlResultVirtualFile
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiAnnotation
import java.util.UUID

/**
 * Provides ArangoDB gutter icons in Java files that use Spring Data ArangoDB annotations.
 *
 * **@Document classes** — shows a database icon next to the class name:
 *  - Green (DataTables icon): collection exists in the active database → click to browse it
 *  - Warning icon: connected but collection not found in the active database
 *  - Gray (DataTables icon): not connected to any ArangoDB instance
 *
 * **@Query methods** — shows a run icon next to the method name:
 *  - Click pre-fills the AQL Console with the query string for review and execution
 */
class SpringDataLineMarkerProvider : LineMarkerProvider {

    companion object {
        private const val DOCUMENT_ANNOTATION = "com.arangodb.springframework.annotation.Document"
        private const val QUERY_ANNOTATION    = "com.arangodb.springframework.annotation.Query"
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Best practice: only process leaf identifier tokens to avoid duplicate markers
        if (element !is PsiIdentifier) return null

        return when (val parent = element.parent) {
            is PsiClass   -> if (parent.nameIdentifier == element) buildDocumentMarker(element, parent) else null
            is PsiMethod  -> if (parent.nameIdentifier == element) buildQueryMarker(element, parent)   else null
            else          -> null
        }
    }

    // ─── @Document ────────────────────────────────────────────────────────────

    private fun buildDocumentMarker(identifier: PsiIdentifier, cls: PsiClass): LineMarkerInfo<*>? {
        val annotation = cls.getAnnotation(DOCUMENT_ANNOTATION) ?: return null
        val collectionName = resolveCollectionName(annotation, cls).takeIf { it.isNotBlank() } ?: return null

        val service   = cls.project.getService(AqlDatabaseService::class.java)
        val connected = service.getAll().isNotEmpty()
        val exists    = connected && service.getCollections().any { it.lookupString == collectionName }

        val icon = if (connected && !exists) AllIcons.General.Warning else AllIcons.Nodes.DataTables

        val tooltip = when {
            !connected -> "ArangoDB collection: '$collectionName' (not connected)"
            exists     -> "Browse ArangoDB collection '$collectionName'  ·  click to open"
            else       -> "Collection '$collectionName' not found in active database"
        }

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            icon,
            { tooltip },
            { _, elt ->
                if (!connected) return@LineMarkerInfo
                val project = elt.project
                val queryId = UUID.randomUUID().toString()
                val query   = "FOR doc IN `$collectionName` LIMIT 100 RETURN doc"
                val file    = AqlResultVirtualFile(collectionName, queryId)
                FileEditorManager.getInstance(project).openFile(file, true)
                ApplicationManager.getApplication().executeOnPooledThread {
                    AqlDataService.with(project).executeQuery(query, queryId)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { "ArangoDB: $collectionName" }
        )
    }

    // ─── @Query ───────────────────────────────────────────────────────────────

    private fun buildQueryMarker(identifier: PsiIdentifier, method: PsiMethod): LineMarkerInfo<*>? {
        val annotation = method.getAnnotation(QUERY_ANNOTATION) ?: return null
        val queryText  = resolveQueryText(annotation).takeIf { it.isNotBlank() } ?: return null

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            AllIcons.Actions.Execute,
            { "Load query in AQL Console  ·  click to open" },
            { _, elt ->
                val project = elt.project
                val data    = ActionEventData(ActionEventData.KEY_QUERY, queryText)
                project.messageBus.syncPublisher(ActionBusEvent.AQL_CONSOLE_LOAD_QUERY).onEvent(data)
                FileEditorManager.getInstance(project)
                    .openFile(AqlConsoleVirtualFile.getInstance(project), true)
            },
            GutterIconRenderer.Alignment.LEFT,
            { "ArangoDB: execute @Query" }
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Resolves the ArangoDB collection name for a `@Document`-annotated class.
     * If the annotation has an explicit `value`, that is used; otherwise the simple
     * class name (lower-camel-cased) is used as Spring Data ArangoDB's default.
     */
    private fun resolveCollectionName(annotation: PsiAnnotation, cls: PsiClass): String {
        val explicit = annotation.findDeclaredAttributeValue("value")
            ?.let { (it as? PsiLiteralExpression)?.value as? String }
            ?.takeIf { it.isNotBlank() }
        return explicit ?: cls.name?.replaceFirstChar { it.lowercaseChar() } ?: ""
    }

    /**
     * Extracts the raw AQL string from a `@Query` annotation.
     * Strips leading/trailing quotes that the PSI literal may include.
     */
    private fun resolveQueryText(annotation: PsiAnnotation): String =
        annotation.findDeclaredAttributeValue("value")
            ?.let { (it as? PsiLiteralExpression)?.value as? String }
            ?: ""
}
