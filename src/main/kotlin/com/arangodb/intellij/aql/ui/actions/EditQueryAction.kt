package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.util.Icons
import com.arangodb.intellij.aql.util.log
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree

class EditQueryAction(private val project: Project, private val queryTree: Tree) :
    BaseQueryAction("Edit Query", "", Icons.ICON_EDIT) {

    override fun actionPerformed(e: AnActionEvent) {
        log.error("NOT IMPLEMENTED YET")
    }
}
