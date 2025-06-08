package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class SetActiveAction(private val project: Project) : AnAction(
    "Set Active",
    "Set ArangoDB database",
    Icons.ICON_SELECTED
) {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        AqlDataService.with(project).setActiveDatabase()
    }
}