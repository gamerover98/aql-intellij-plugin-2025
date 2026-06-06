package com.arangodb.intellij.aql.ui.panels

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

/**
 * Tabular view for AQL query results.
 *
 * - Derives columns from the union of all JSON-object fields in the result array.
 * - Metadata fields (_id, _key, _rev, _from, _to, _class) are pinned to the left.
 * - `_class` column values render as clickable hyperlinks that navigate to the
 *   matching Java/Kotlin class in the project.
 * - Columns are sortable by clicking the header.
 */
class ResultTablePanel(private val project: Project) : JPanel(BorderLayout()) {

    /** ArangoDB metadata keys pinned to the left of the column list. */
    private val META_KEYS = listOf("_id", "_key", "_rev", "_from", "_to", "_class")

    private val tableModel = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(col: Int) = String::class.java
    }
    private val table = JBTable(tableModel)
    private val mapper = ObjectMapper()

    init {
        background = JBColor.background()

        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.fillsViewportHeight = true
        table.rowHeight = JBUI.scale(22)
        table.setDefaultRenderer(String::class.java, ClassAwareCellRenderer())
        table.rowSorter = TableRowSorter(tableModel)

        // _class click → navigate; cursor feedback on hover
        val mouseHandler = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val col = table.columnAtPoint(e.point)
                if (row < 0 || col < 0) return
                val colName = table.columnModel.getColumn(col).headerValue as? String ?: return
                if (colName != "_class") return
                val modelRow = table.convertRowIndexToModel(row)
                val value = tableModel.getValueAt(modelRow, col) as? String ?: return
                navigateToClass(value)
            }
            override fun mouseMoved(e: MouseEvent) = updateCursor(e)
        }
        table.addMouseListener(mouseHandler)
        table.addMouseMotionListener(mouseHandler)

        add(JBScrollPane(table), BorderLayout.CENTER)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setData(json: String) {
        tableModel.rowCount = 0
        tableModel.columnCount = 0
        if (json.isBlank()) return

        try {
            val root = mapper.readTree(json)
            val items = when {
                root.isArray  -> root.toList()
                root.isObject -> listOf(root)
                else          -> return
            }
            if (items.isEmpty()) return

            // Build ordered column list: meta fields first, then others alphabetically
            val allKeys = linkedSetOf<String>()
            items.forEach { item ->
                if (item.isObject) item.fieldNames().forEach { allKeys.add(it) }
            }
            val columns = (META_KEYS.filter { it in allKeys } +
                           allKeys.filterNot { it in META_KEYS }.sorted())
                .toTypedArray<Any>()

            tableModel.setColumnIdentifiers(columns)

            // Fill rows
            items.forEach { item ->
                val row: Array<Any?> = columns.map { col ->
                    if (!item.isObject) return@map item.toString()
                    val node = item.get(col as String)
                    when {
                        node == null || node.isNull -> null
                        node.isTextual             -> node.asText()
                        else                       -> node.toString()
                    }
                }.toTypedArray()
                tableModel.addRow(row)
            }

            // Auto-size columns (sample up to 30 rows for content width)
            val headerFm = table.tableHeader.getFontMetrics(table.tableHeader.font)
            val cellFm   = table.getFontMetrics(table.font)
            for (i in columns.indices) {
                val col = table.columnModel.getColumn(i)
                val headerW = headerFm.stringWidth(columns[i].toString()) + JBUI.scale(24)
                val contentW = (0 until minOf(tableModel.rowCount, 30))
                    .mapNotNull { r -> tableModel.getValueAt(r, i) as? String }
                    .maxOfOrNull { s -> cellFm.stringWidth(s) + JBUI.scale(16) }
                    ?: JBUI.scale(60)
                col.preferredWidth = maxOf(headerW, contentW).coerceAtMost(JBUI.scale(320))
            }
        } catch (_: Exception) { /* malformed JSON — leave table empty */ }
    }

    fun clear() {
        tableModel.rowCount = 0
        tableModel.columnCount = 0
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun navigateToClass(fqn: String) {
        val shortName = fqn.trim().substringAfterLast('.')
        val scope = GlobalSearchScope.projectScope(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            val vf: VirtualFile? = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
                FilenameIndex.getVirtualFilesByName("$shortName.java", scope).firstOrNull()
                    ?: FilenameIndex.getVirtualFilesByName("$shortName.kt", scope).firstOrNull()
            }
            vf?.let { ApplicationManager.getApplication().invokeLater { OpenFileDescriptor(project, it).navigate(true) } }
        }
    }

    private fun updateCursor(e: MouseEvent) {
        val col = table.columnAtPoint(e.point)
        val colName = if (col >= 0) table.columnModel.getColumn(col).headerValue as? String else null
        table.cursor = if (colName == "_class") Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                       else Cursor.getDefaultCursor()
    }

    // ─── Cell renderer ────────────────────────────────────────────────────────

    private inner class ClassAwareCellRenderer : DefaultTableCellRenderer() {

        init {
            horizontalAlignment = SwingConstants.LEFT
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            // Let super set background, selection colors, etc.
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            val colName = table.columnModel.getColumn(column).headerValue as? String

            when {
                value == null -> {
                    text = "null"
                    font = font.deriveFont(Font.ITALIC)
                    if (!isSelected) foreground = JBColor.GRAY
                }
                colName == "_class" && value is String && value.isNotBlank() -> {
                    font = font.deriveFont(Font.PLAIN)
                    if (!isSelected) foreground = JBColor(Color(0x0066CC), Color(0x4A9EFF))
                    text = "<html><u>${esc(value)}</u></html>"
                }
                else -> {
                    font = font.deriveFont(Font.PLAIN)
                    // foreground already set by super
                    text = value.toString()
                }
            }
            return this
        }

        private fun esc(s: String) =
            s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
