package com.arangodb.intellij.aql.ui.renderers

import com.google.common.base.Strings
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class AqlNodeRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val userObject = (value as? DefaultMutableTreeNode)?.userObject
        if (userObject is AqlNodeModel) {
            val name = if (Strings.isNullOrEmpty(userObject.name)) {
                userObject.displayName ?: ""
            } else {
                "${userObject.name} :${userObject.displayName}"
            }
            append(name, userObject.getStyle(), true)
            setIcon(userObject.getIcon())
            toolTipText = userObject.type.name
        } else {
            append(
                tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus),
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            )
        }
    }
}
