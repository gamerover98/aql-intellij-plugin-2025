package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class AddServerAction(private val project: Project) : AnAction(
    "Add Server",
    "Add ArangoDB server instance",
    Icons.ICON_ARANGO_SMALL
) {
    override fun actionPerformed(e: AnActionEvent) {
        AqlDataService.with(project).showServerDialog()
    }
}

