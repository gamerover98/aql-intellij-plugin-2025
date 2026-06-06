package com.arangodb.intellij.aql.actions

import com.arangodb.intellij.aql.model.AqlQuery
import com.arangodb.intellij.aql.ui.dialogs.AqlParameterDialog
import com.arangodb.intellij.aql.ui.console.AqlConsoleVirtualFile
import com.arangodb.intellij.aql.ui.console.AqlResultVirtualFile
import com.arangodb.intellij.aql.util.AQL_LANGUAGE_ID
import com.arangodb.intellij.aql.util.AqlUtils
import com.arangodb.intellij.aql.util.log
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.util.UUID

abstract class AqlQueryAction : AnAction() {

    protected fun runQueryAction(event: AnActionEvent, type: AqlDataService.QueryType) {
        val project = getEventProject(event)
        if (!canExecute(project, event)) return

        val charSequence = extractQuery(event)
        if (charSequence.isEmpty()) {
            log.warn("No query found/selected")
            return
        }
        val names = AqlUtils.extractParameterNames(charSequence, project!!)
        val service = AqlDataService.with(project)
        val query = charSequence.toString()

        val existing = service.getExistingQueryForValue(query)
        if (existing != null) {
            execute(type, service, query, existing.getParameters(), project)
            return
        }

        if (names.isNotEmpty()) {
            val element = event.dataContext.getData(CommonDataKeys.PSI_ELEMENT)
            val dialog = AqlParameterDialog(project, names, element)
            if (dialog.showAndGet()) {
                val data = dialog.getData()
                execute(type, service, query, data, project)
                saveQuery(event, service, query, data)
            } else {
                log.error("No parameters defined")
            }
            return
        }

        execute(type, service, query, emptyMap(), project)
        saveQuery(event, service, query, emptyMap())
    }

    private fun execute(type: AqlDataService.QueryType, service: AqlDataService, query: String,
                        data: Map<String, String>, project: Project) {
        val queryId = UUID.randomUUID().toString()
        val label   = if (type == AqlDataService.QueryType.QUERY) "Result" else "Explain"
        val resultFile = AqlResultVirtualFile(label, queryId)
        FileEditorManager.getInstance(project).openFile(resultFile, true)
        if (type == AqlDataService.QueryType.QUERY) service.executeQuery(query, data, queryId)
        else service.explainQuery(query, data, queryId)
    }

    protected fun saveQuery(event: AnActionEvent, service: AqlDataService, query: String, data: Map<String, String>) {
        val file = event.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
        val name = if (file == null) "Query" else "${file.name}_query"
        service.saveQuery(AqlQuery(name, query, data.toMutableMap()))
    }

    protected fun showConsole(project: Project) {
        FileEditorManager.getInstance(project).openFile(AqlConsoleVirtualFile.getInstance(project), true)
    }

    protected fun canExecute(project: Project?, event: AnActionEvent): Boolean {
        val editor = event.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE)
        if (project == null || editor == null) return false
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return false
        return psiFile.language.id == AQL_LANGUAGE_ID
    }

    fun extractQuery(event: AnActionEvent): CharSequence {
        val editor = event.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE) ?: return ""
        val caret = editor.caretModel.primaryCaret
        return if (caret.hasSelection()) caret.selectedText ?: "" else editor.document.charsSequence
    }
}
