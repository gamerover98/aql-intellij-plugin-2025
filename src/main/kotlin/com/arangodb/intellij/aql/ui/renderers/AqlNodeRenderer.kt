package com.arangodb.intellij.aql.ui.renderers

import com.google.common.base.Strings
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom tree cell renderer for the ArangoDB connection panel.
 *
 * Rendering rules:
 *  - DATABASE nodes that are "active" show a small green "active" badge (A2)
 *  - COLLECTION/EDGE nodes show a grayed "• count" suffix once counts are loaded (A5)
 *  - CATEGORY nodes (e.g. the "System (N)" folder) are rendered in gray
 *  - Tooltips reflect node type + document count when available
 */
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

            // A2: Active-database badge — small colored "active" label after the db name
            if (userObject.type == AqlNodeModel.Type.DATABASE && userObject.isSelected) {
                append(
                    "  active",
                    SimpleTextAttributes(
                        SimpleTextAttributes.STYLE_SMALLER or SimpleTextAttributes.STYLE_ITALIC,
                        JBColor(Color(0x2E7D32), Color(0x66BB6A))
                    )
                )
            }

            // A5: Document count — shown once the background loader fills it in
            val cnt = userObject.count
            if (cnt != null && cnt >= 0 &&
                (userObject.type == AqlNodeModel.Type.COLLECTION || userObject.type == AqlNodeModel.Type.EDGE)
            ) {
                append("  •  ${formatCount(cnt)}", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
            }

            setIcon(userObject.getIcon())
            toolTipText = buildTooltip(userObject)
        } else {
            append(
                tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus),
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            )
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * A6: Build tooltip text.
     * - When [AqlNodeModel.tooltipLines] is populated (SERVER/DATABASE) → HTML multi-line.
     * - For COLLECTION/EDGE with a loaded count → plain "Type — N documents".
     * - Otherwise → just the type label.
     */
    private fun buildTooltip(model: AqlNodeModel): String {
        if (model.tooltipLines.isNotEmpty()) {
            return "<html>${model.tooltipLines.joinToString("<br>")}</html>"
        }
        val typeLabel = model.type.name
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
        val cnt = model.count
        return when {
            cnt != null && cnt >= 0 ->
                "$typeLabel — ${java.text.NumberFormat.getNumberInstance().format(cnt)} documents"
            else -> typeLabel
        }
    }

    /**
     * Compact count display:
     *  - < 1 000      → raw number ("42")
     *  - ≥ 1 000      → rounded to thousands ("1k", "12k")
     *  - ≥ 1 000 000  → rounded to millions ("3M")
     */
    private fun formatCount(count: Long): String = when {
        count >= 1_000_000L -> "${count / 1_000_000}M"
        count >= 1_000L     -> "${count / 1_000}k"
        else                -> count.toString()
    }
}
