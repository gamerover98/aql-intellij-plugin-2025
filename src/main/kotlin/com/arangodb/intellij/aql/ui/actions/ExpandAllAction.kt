package com.arangodb.intellij.aql.ui.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JTree

class ExpandAllAction(private val tree: JTree) : AnAction(
    "Expand All",
    "Expand all nodes in the tree",
    AllIcons.Actions.Expandall
) {
    override fun actionPerformed(e: AnActionEvent) {
        for (row in 0 until tree.rowCount) {
            tree.expandRow(row)
        }
    }
}