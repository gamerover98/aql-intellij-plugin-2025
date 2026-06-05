package com.arangodb.intellij.aql.ui.panels

import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.services.AqlResultService
import com.arangodb.intellij.aql.ui.MessageView
import com.arangodb.intellij.aql.util.AqlUtils
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.codeStyle.CodeStyleManager
import javax.swing.JComponent

class JsonPanel(project: Project) : ConsoleViewImpl(project, true), Disposable, MessageView {

    val consoleComponent: JComponent = component

    init {
        Disposer.register(project, this)
    }

    override fun dispose() = super.dispose()

    fun getCurrentText(): String = editor?.document?.text ?: ""

    override fun onMessage(data: ActionEventData, project: Project) {
        val charSequence = data.get(ActionEventData.KEY_RESULT) ?: return
        project.getService(AqlResultService::class.java).lastResult = charSequence
        WriteAction.run<Exception> {
            val file = AqlUtils.createDummyJsonFile(charSequence, project) ?: return@run
            val formatted = CodeStyleManager.getInstance(project).reformat(file)
            editor?.document?.setText(formatted.text)
        }
    }

    override fun onClean(project: Project) {
        WriteAction.run<Exception> {
            editor?.document?.setText("")
        }
    }
}
