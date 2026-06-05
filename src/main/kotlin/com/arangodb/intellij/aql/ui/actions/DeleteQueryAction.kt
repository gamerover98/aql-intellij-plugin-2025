package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.util.Icons
import com.arangodb.intellij.aql.util.log
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree

class DeleteQueryAction(private val project: Project, private val queryTree: Tree) :
    BaseQueryAction("Delete Query", "", Icons.ICON_DELETE) {

    override fun actionPerformed(e: AnActionEvent) {
        val query = getSelectedQuery(project, queryTree)
        if (query == null) log.error("Nothing selected")
        else AqlDataService.with(project).deleteQuery(query.name ?: "")
    }
}
