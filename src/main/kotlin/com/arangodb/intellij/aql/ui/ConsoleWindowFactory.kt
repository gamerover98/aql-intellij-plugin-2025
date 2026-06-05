package com.arangodb.intellij.aql.ui

import com.arangodb.intellij.aql.ui.windows.AqlConsoleWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ConsoleWindowFactory : ToolWindowFactory {

    private var dataWindow: AqlConsoleWindow? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        dataWindow = AqlConsoleWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(dataWindow!!.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
        Disposer.register(project, dataWindow!!)
    }
}
