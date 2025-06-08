package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class RefreshSchemeAction(private val project: Project) : AnAction(
    "Refresh Server",
    "Refresh server collections",
    AllIcons.Actions.Refresh
) {
    override fun actionPerformed(e: AnActionEvent) {
        AqlDataService.with(project).refreshSchema()
    }
}