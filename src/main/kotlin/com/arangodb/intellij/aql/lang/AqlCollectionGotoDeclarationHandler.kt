package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.ui.console.AqlConsoleVirtualFile
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement

/**
 * Handles Ctrl+Click on ArangoDB collection, graph, or view names in AQL files.
 *
 * Instead of navigating to a PSI element (collections are not source code),
 * this handler executes a browse query that shows the collection's data in the
 * ArangoDB Console tool window.
 */
class AqlCollectionGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val namedElement: AqlNamedElement = when {
            element is AqlNamedElement && element.aqlType == AqlMixinType.ID -> element
            element.parent is AqlNamedElement &&
                    (element.parent as AqlNamedElement).aqlType == AqlMixinType.ID ->
                element.parent as AqlNamedElement
            else -> return null
        }

        val name = namedElement.text
        val project = element.project
        val service = project.getService(AqlDatabaseService::class.java)

        val isCollection = service.getCollections().any { it.lookupString == name }
        val isGraph = service.getGraphs().any { it.lookupString == name }
        val isView = service.getSearchViews().any { it.lookupString == name }

        if (!isCollection && !isGraph && !isView) return null

        val query = when {
            isGraph -> "FOR v, e IN 1..1 ANY null GRAPH `$name` LIMIT 100 RETURN {vertex: v, edge: e}"
            isView -> "FOR doc IN `$name` SEARCH true LIMIT 100 RETURN doc"
            else -> "FOR doc IN `$name` LIMIT 100 RETURN doc"
        }

        // Execute on EDT after current action completes, then open the console editor tab
        ApplicationManager.getApplication().invokeLater {
            AqlDataService.with(project).executeQuery(query)
            FileEditorManager.getInstance(project)
                .openFile(AqlConsoleVirtualFile.getInstance(project), true)
        }

        // Return empty array: we handled navigation ourselves (data shown in console)
        return PsiElement.EMPTY_ARRAY
    }
}
