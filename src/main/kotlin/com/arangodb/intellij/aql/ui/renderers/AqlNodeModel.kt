package com.arangodb.intellij.aql.ui.renderers

import com.arangodb.intellij.aql.util.Icons
import com.intellij.icons.AllIcons
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

class AqlNodeModel @JvmOverloads constructor(
    var name: String? = null,
    var displayName: String? = null,
    var type: Type = Type.COLLECTION
) {
    enum class Type { SERVER, DATABASE, COLLECTION, GRAPH, VIEW, EDGE, CATEGORY }

    var isSelected: Boolean = false

    /**
     * Document count for COLLECTION/EDGE nodes.
     * `null` = not yet loaded; negative = load failed or not applicable.
     */
    var count: Long? = null

    fun getIcon(): Icon = when (type) {
        Type.SERVER     -> Icons.ICON_ARANGO_SMALL
        Type.DATABASE   -> Icons.ICON_DATABASE
        Type.COLLECTION -> Icons.ICON_COLLECTION
        Type.GRAPH      -> Icons.ICON_GRAPH
        Type.VIEW       -> Icons.ICON_VIEW
        Type.EDGE       -> Icons.ICON_EDGE
        Type.CATEGORY   -> AllIcons.Nodes.Folder
    }

    fun getStyle(): SimpleTextAttributes {
        if (isSelected) return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
        val dn = displayName ?: ""
        if (dn.startsWith("_")) return SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
        return when (type) {
            Type.COLLECTION -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
            Type.EDGE       -> SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
            Type.CATEGORY   -> SimpleTextAttributes.GRAYED_ATTRIBUTES
            else            -> SimpleTextAttributes.REGULAR_ATTRIBUTES
        }
    }

    override fun toString(): String = displayName ?: ""
}
