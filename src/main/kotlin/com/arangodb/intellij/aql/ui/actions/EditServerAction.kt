package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/** Opens the server-settings dialog pre-filled with the current configuration. */
class EditServerAction(private val project: Project) : AnAction(
    "Edit Server",
    "Edit the current ArangoDB server configuration",
    Icons.ICON_EDIT
) {
    override fun actionPerformed(e: AnActionEvent) {
        AqlDataService.with(project).showServerDialog()
    }
}
