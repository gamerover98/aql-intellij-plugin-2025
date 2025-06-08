package com.arangodb.intellij.aql.ui.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JTree

class CollapseAllAction(private val tree: JTree) : AnAction(
    "Collapse All",
    "Collapse all nodes in the tree",
    AllIcons.Actions.Collapseall
) {
    override fun actionPerformed(e: AnActionEvent) {
        for (row in tree.rowCount - 1 downTo 0) {
            tree.collapseRow(row)
        }
    }
}