package com.arangodb.intellij.aql.ui.renderers

import com.google.common.base.Strings
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class AqlQueryRenderer : ColoredTreeCellRenderer() {

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
        if (userObject is AqlQueryModel) {
            val name = extractName(userObject.name, userObject.getParameters())
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

    private fun extractName(name: String?, parameters: Map<String, String>): String {
        if (Strings.isNullOrEmpty(name)) return "<unknown>"
        if (parameters.isEmpty()) return name!!
        val sb = StringBuilder(" ( ")
        parameters.forEach { (k, v) -> sb.append("$k:${v} ") }
        sb.append(')')
        return name + sb
    }
}
