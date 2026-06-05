package com.arangodb.intellij.aql.util

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.toolWindow.ServerToolWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

class DataSourceWindowCallback(private val project: Project) : ActionMessageCallback {
    override fun call() {
        ToolWindowManager.getInstance(project).getToolWindow(ServerToolWindow.WINDOW_ID)?.activate(null, true)
        AqlDataService.with(project).showServerDialog()
    }
}
