package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/** Shows a confirmation dialog and removes the server configuration on "Yes". */
class RemoveServerAction(private val project: Project) : AnAction(
    "Remove Server",
    "Remove the current ArangoDB server configuration",
    Icons.ICON_DELETE
) {
    override fun actionPerformed(e: AnActionEvent) {
        val choice = Messages.showYesNoDialog(
            project,
            "Remove the current server configuration?\nThis action cannot be undone.",
            "Remove Server",
            Messages.getQuestionIcon()
        )
        if (choice == Messages.YES) {
            AqlDataService.with(project).removeServer()
        }
    }
}
