package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.ui.dialogs.AqlEditQueryDialog
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree

class EditQueryAction(private val project: Project, private val queryTree: Tree) :
    BaseQueryAction("Edit Query", "Edit the selected saved query", Icons.ICON_EDIT) {

    override fun actionPerformed(e: AnActionEvent) {
        val query = getSelectedQuery(project, queryTree) ?: return
        val dialog = AqlEditQueryDialog(project, query)
        if (dialog.showAndGet()) {
            AqlDataService.with(project).updateQuery(dialog.getUpdatedQuery())
        }
    }
}
