package com.arangodb.intellij.aql.actions

import com.arangodb.intellij.aql.model.AqlQuery
import com.arangodb.intellij.aql.ui.dialogs.AqlParameterDialog
import com.arangodb.intellij.aql.ui.windows.AqlConsoleWindow
import com.arangodb.intellij.aql.util.AQL_LANGUAGE_ID
import com.arangodb.intellij.aql.util.AqlUtils
import com.arangodb.intellij.aql.util.log
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

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
            execute(type, service, query, existing.getParameters())
            return
        }

        if (names.isNotEmpty()) {
            val element = event.dataContext.getData(CommonDataKeys.PSI_ELEMENT)
            val dialog = AqlParameterDialog(project, names, element)
            if (dialog.showAndGet()) {
                val data = dialog.getData()
                execute(type, service, query, data)
                showConsole(project)
                saveQuery(event, service, query, data)
            } else {
                log.error("No parameters defined")
            }
            return
        }

        execute(type, service, query, emptyMap())
        saveQuery(event, service, query, emptyMap())
        showConsole(project)
    }

    private fun execute(type: AqlDataService.QueryType, service: AqlDataService, query: String, data: Map<String, String>) {
        if (type == AqlDataService.QueryType.QUERY) service.executeQuery(query, data)
        else service.explainQuery(query, data)
    }

    protected fun saveQuery(event: AnActionEvent, service: AqlDataService, query: String, data: Map<String, String>) {
        val file = event.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
        val name = if (file == null) "Query" else "${file.name}_query"
        service.saveQuery(AqlQuery(name, query, data.toMutableMap()))
    }

    protected fun showConsole(project: Project) {
        ToolWindowManager.getInstance(project).getToolWindow(AqlConsoleWindow.WINDOW_ID)?.activate(null, true)
    }

    protected fun canExecute(project: Project?, event: AnActionEvent): Boolean {
        val editor = event.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE)
        if (project == null || editor == null) return false
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return false
        if (psiFile.language.id != AQL_LANGUAGE_ID) return false
        return ToolWindowManager.getInstance(project).getToolWindow(AqlConsoleWindow.WINDOW_ID) != null
    }

    fun extractQuery(event: AnActionEvent): CharSequence {
        val editor = event.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE) ?: return ""
        val caret = editor.caretModel.primaryCaret
        return if (caret.hasSelection()) caret.selectedText ?: "" else editor.document.charsSequence
    }
}
