package com.arangodb.intellij.aql.ui.panels

import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.services.AqlResultService
import com.arangodb.intellij.aql.ui.MessageView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

class JsonPanel(project: Project) : ConsoleViewImpl(project, true), Disposable, MessageView {

    val consoleComponent: JComponent = component

    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    init {
        Disposer.register(project, this)
    }

    override fun dispose() = super.dispose()

    fun getCurrentText(): String = editor?.document?.text ?: ""

    override fun onMessage(data: ActionEventData, project: Project) {
        val raw = data.get(ActionEventData.KEY_RESULT) ?: return
        project.getService(AqlResultService::class.java).lastResult = raw
        val pretty = try {
            mapper.writeValueAsString(mapper.readTree(raw))
        } catch (_: Exception) {
            raw
        }
        WriteAction.run<Exception> {
            editor?.document?.setText(pretty)
        }
    }

    override fun onClean(project: Project) {
        WriteAction.run<Exception> {
            editor?.document?.setText("")
        }
    }
}
