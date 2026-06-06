package com.arangodb.intellij.aql.toolWindow

import com.arangodb.entity.IndexEntity
import com.arangodb.intellij.aql.ui.renderers.AqlNodeModel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import java.text.NumberFormat
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableModel

/**
 * C3: Detail panel shown below the connection tree.
 *
 * Displays metadata for the selected COLLECTION or EDGE node:
 *  – Header: type icon, name, document count (already loaded asynchronously by A5)
 *  – Index table: type | name | fields | unique | sparse
 *
 * States:
 *  [showEmpty]   — default; prompts the user to select a collection
 *  [showLoading] — while index data is fetched from ArangoDB
 *  [showDetails] — fully populated view
 */
class CollectionDetailPanel : JPanel(BorderLayout()) {

    private val header = JLabel("Select a collection to view details").apply {
        horizontalAlignment = SwingConstants.LEFT
        border = JBUI.Borders.empty(4, 8)
        font = font.deriveFont(Font.ITALIC)
        foreground = JBColor.GRAY
    }

    private val TABLE_COLUMNS = arrayOf("Index type", "Name", "Fields", "Unique", "Sparse")
    private val tableModel = object : DefaultTableModel(TABLE_COLUMNS, 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val table = JBTable(tableModel).apply {
        setShowGrid(false)
        tableHeader.reorderingAllowed = false
        // Reasonable default column widths
        columnModel.getColumn(0).preferredWidth = 110
        columnModel.getColumn(1).preferredWidth = 130
        columnModel.getColumn(2).preferredWidth = 260
        columnModel.getColumn(3).preferredWidth = 60
        columnModel.getColumn(4).preferredWidth = 60
    }

    /** Shows sampled document field names below the index table. */
    private val fieldsLabel = JLabel("Fields: —").apply {
        horizontalAlignment = SwingConstants.LEFT
        border = JBUI.Borders.empty(4, 8)
        font = font.deriveFont(Font.PLAIN)
    }

    init {
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
        add(header, BorderLayout.NORTH)
        add(JBScrollPane(table).apply {
            border = JBUI.Borders.empty()
        }, BorderLayout.CENTER)
        add(fieldsLabel, BorderLayout.SOUTH)
    }

    // ─── State transitions ────────────────────────────────────────────────────

    fun showEmpty() {
        header.icon = null
        header.text = "Select a collection to view details"
        header.font = header.font.deriveFont(Font.ITALIC)
        header.foreground = JBColor.GRAY
        tableModel.rowCount = 0
        fieldsLabel.text = "Fields: —"
    }

    fun showLoading(collectionName: String) {
        header.icon = null
        header.text = "Loading $collectionName…"
        header.font = header.font.deriveFont(Font.ITALIC)
        header.foreground = JBColor.GRAY
        tableModel.rowCount = 0
        fieldsLabel.text = "Fields: loading…"
    }

    fun showDetails(model: AqlNodeModel, indexes: List<IndexEntity>, fields: List<String> = emptyList()) {
        val name      = model.displayName ?: ""
        val count     = model.count
        val countPart = if (count != null && count >= 0)
            "  —  ${NumberFormat.getNumberInstance().format(count)} documents"
        else ""

        header.icon = model.getIcon()
        header.text = "<html><b>$name</b><span style='color:gray'>$countPart</span></html>"
        header.font = header.font.deriveFont(Font.PLAIN)
        header.foreground = JBColor.foreground()

        tableModel.rowCount = 0
        for (idx in indexes.sortedWith(compareBy({ it.type?.toString() ?: "" }, { it.name ?: "" }))) {
            tableModel.addRow(arrayOf(
                idx.type?.toString()?.lowercase()?.replaceFirstChar { it.uppercaseChar() } ?: "",
                idx.name ?: "",
                idx.fields?.joinToString(", ") ?: "",
                if (idx.unique == true) "✓" else "–",
                if (idx.sparse == true) "✓" else "–"
            ))
        }

        fieldsLabel.text = if (fields.isEmpty()) "Fields: —"
                           else "<html><b>Fields:</b> ${fields.joinToString(", ")}</html>"
    }
}
