package com.arangodb.intellij.aql.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * @deprecated The AQL Console has been moved to an editor tab.
 * See [com.arangodb.intellij.aql.ui.console.AqlConsoleEditorProvider] and
 * [com.arangodb.intellij.aql.ui.console.AqlConsoleFileEditor].
 * This factory is no longer registered in plugin.xml.
 */
@Deprecated("AQL Console is now an editor tab — see AqlConsoleEditorProvider")
class ConsoleWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // No-op: this factory is no longer registered in plugin.xml.
        // The AQL Console is now opened as an editor tab via AqlConsoleVirtualFile.
    }
}
