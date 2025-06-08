package com.arangodb.intellij.aql.toolWindow

import com.arangodb.intellij.aql.ArangoBundle
import com.arangodb.intellij.aql.services.ArangoProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton

/**
 * (DEVELOPMENT) Test tool window factory for the ArangoDB plugin.
 */
class ArangoToolWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val arangoToolWindow = ArangoToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(arangoToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    class ArangoToolWindow(toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<ArangoProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(ArangoBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(ArangoBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = ArangoBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}