package com.arangodb.intellij.aql.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton

/**
 * This class is responsible for creating the tool window content for the ArangoDB plugin.
 */
class ServerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow
            .contentManager
            .addContent(
                ContentFactory
                    .getInstance()
                    .createContent(
                        ServerToolWindow(project).getContent(),
                        "ArangoDB Connections",
                        false
                    )
            )
    }
}