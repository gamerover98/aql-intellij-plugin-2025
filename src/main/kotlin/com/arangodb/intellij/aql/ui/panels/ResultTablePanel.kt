package com.arangodb.intellij.aql.ui.panels

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.icons.AllIcons
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
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.RowFilter
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

/**
 * Tabular view for AQL query results.
 *
 * Features:
 *  - Automatic row limit: only the first [PAGE_SIZE] rows are shown by default;
 *    a "Show All" button in the status bar reveals the full result set, and a
 *    "Show [PAGE_SIZE]" button returns to the paged view.
 *  - Live filter field: instantly narrows visible rows by any case-insensitive
 *    text match across all columns (regex-escaped, so plain text is safe).
 *  - `_class` always first column: shows only the simple class name; the tooltip
 *    and the status bar below the table show the full FQN when the cell is
 *    selected. Double-click navigates to the matching .java/.kt source file.
 *  - Metadata fields (_class, _id, _key, _rev, _from, _to) pinned left; other
 *    fields follow alphabetically.
 *  - Columns auto-sized to content (sampled from the first 30 rows).
 *  - Single-cell selection; Ctrl+C copies the cell value.
 *  - Toolbar: Copy Row | Copy JSON | Copy as String.
 *  - Right-click context menu: Copy Cell, Copy Row as JSON, Copy All as JSON,
 *    Copy All as String, Navigate to Class.
 */
class ResultTablePanel(private val project: Project) : JPanel(BorderLayout()) {

    companion object {
        /** Maximum rows shown before the user opts in to "Show All". */
        private const val PAGE_SIZE = 500
    }

    /** Pinned metadata columns — _class always first. */
    private val META_KEYS = listOf("_class", "_id", "_key", "_rev", "_from", "_to")

    private val mapper = ObjectMapper()
    private var lastRawJson = ""

    // ─── Pagination state ─────────────────────────────────────────────────────
    private var allItems: List<JsonNode> = emptyList()
    private var columnNames: List<String> = emptyList()
    private var showAll = false

    // ─── Toolbar buttons (fields needed for copy feedback) ────────────────────
    private lateinit var copyRowBtn: JButton
    private lateinit var copyJsonBtn: JButton
    private lateinit var copyStringBtn: JButton
    private var buttonFeedbackTimer: Timer? = null

    // ─── Search field ─────────────────────────────────────────────────────────
    private val searchField = JTextField().apply {
        preferredSize = JBUI.size(200, 24)
        toolTipText = "Filter rows (case-insensitive, plain text)"
    }

    // ─── Model & sorter ───────────────────────────────────────────────────────
    private val tableModel = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(col: Int) = String::class.java
    }
    private val rowSorter = TableRowSorter(tableModel)

    // ─── Table ────────────────────────────────────────────────────────────────

    /**
     * Custom table that shows the full FQN as a tooltip for `_class` cells
     * (the cell itself only displays the short class name).
     */
    private val table = object : JBTable(tableModel) {
        override fun getToolTipText(e: MouseEvent): String? {
            val row = rowAtPoint(e.point)
            val col = columnAtPoint(e.point)
            if (row < 0 || col < 0) return super.getToolTipText(e)
            val colName = columnModel.getColumn(col).headerValue as? String
                ?: return super.getToolTipText(e)
            if (colName != "_class") return super.getToolTipText(e)
            val modelRow = convertRowIndexToModel(row)
            return (model.getValueAt(modelRow, col) as? String)?.takeIf { it.isNotBlank() }
                ?: super.getToolTipText(e)
        }
    }

    // ─── Bottom bar widgets ───────────────────────────────────────────────────

    /** Shows full _class FQN on selection; flashes "✓ Copied" on context-menu copy. */
    private val statusLabel = JLabel(" ").apply {
        border = JBUI.Borders.empty(1, 8)
        foreground = JBColor.GRAY
        font = font.deriveFont(Font.ITALIC, (font.size - 1).toFloat())
        horizontalAlignment = SwingConstants.LEFT
    }
    private var statusFeedbackTimer: Timer? = null

    /** Shows "Showing X of N rows" / "Showing all N rows". */
    private val paginationLabel = JLabel(" ").apply {
        border = JBUI.Borders.empty(1, 8)
        foreground = JBColor.GRAY
        font = font.deriveFont((font.size - 1).toFloat())
    }

    private lateinit var showAllBtn: JButton
    private lateinit var show500Btn: JButton

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        background = JBColor.background()

        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        table.fillsViewportHeight = true
        table.rowHeight = JBUI.scale(22)
        table.setDefaultRenderer(String::class.java, ClassAwareCellRenderer())
        table.rowSorter = rowSorter

        // Single-cell selection
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.cellSelectionEnabled = true

        // Ctrl+C → copy selected cell value
        table.actionMap.put("copy", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) = copySelectedCell()
        })
        table.getInputMap(JTable.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy"
        )

        // Mouse: right-click preselect, double-click navigate, cursor update
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) preselectCell(e)
            }
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) onDoubleClick(e)
            }
            override fun mouseMoved(e: MouseEvent) = updateCursor(e)
        })
        table.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) = updateCursor(e)
        })

        table.componentPopupMenu = buildContextMenu()

        // Status label: update on cell selection change
        table.selectionModel.addListSelectionListener { updateStatusLabel() }
        table.columnModel.selectionModel.addListSelectionListener { updateStatusLabel() }

        // Search field → live RowFilter
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter()
            override fun removeUpdate(e: DocumentEvent) = applyFilter()
            override fun changedUpdate(e: DocumentEvent) = applyFilter()
        })

        add(buildToolbar(), BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
        add(buildBottomBar(), BorderLayout.SOUTH)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setData(json: String) {
        lastRawJson = json
        allItems = emptyList()
        columnNames = emptyList()
        showAll = false
        tableModel.rowCount = 0
        tableModel.columnCount = 0

        if (json.isBlank()) { updatePaginationLabel(); updateStatusLabel(); return }

        try {
            val root = mapper.readTree(json)
            val items = when {
                root.isArray  -> root.toList()
                root.isObject -> listOf(root)
                else          -> return
            }
            if (items.isEmpty()) return

            // Build ordered columns: meta keys first (_class always first), rest α-sorted
            val allKeys = linkedSetOf<String>()
            items.forEach { item -> if (item.isObject) item.fieldNames().forEach { allKeys.add(it) } }
            columnNames = META_KEYS.filter { it in allKeys } + allKeys.filterNot { it in META_KEYS }.sorted()
            allItems = items

            tableModel.setColumnIdentifiers(columnNames.toTypedArray<Any>())
            repopulateRows()
            autoSizeColumns()
        } catch (_: Exception) { /* malformed JSON — leave table empty */ }
    }

    fun clear() {
        lastRawJson = ""
        allItems = emptyList()
        columnNames = emptyList()
        showAll = false
        tableModel.rowCount = 0
        tableModel.columnCount = 0
        searchField.text = ""
        statusLabel.text = " "
        statusLabel.foreground = JBColor.GRAY
        paginationLabel.text = " "
        showAllBtn.isVisible = false
        show500Btn.isVisible = false
    }

    // ─── Pagination ───────────────────────────────────────────────────────────

    /**
     * Clears and repopulates the table model from [allItems], respecting the
     * current [showAll] flag and the [PAGE_SIZE] limit.
     */
    private fun repopulateRows() {
        tableModel.rowCount = 0
        val items = if (showAll || allItems.size <= PAGE_SIZE) allItems else allItems.take(PAGE_SIZE)
        items.forEach { item ->
            val row: Array<Any?> = columnNames.map { col ->
                if (!item.isObject) return@map item.toString()
                val node = item.get(col)
                when {
                    node == null || node.isNull -> null
                    node.isTextual             -> node.asText()
                    else                       -> node.toString()
                }
            }.toTypedArray()
            tableModel.addRow(row)
        }
        updatePaginationLabel()
    }

    private fun updatePaginationLabel() {
        val total = allItems.size
        if (total == 0) {
            paginationLabel.text = " "
            showAllBtn.isVisible = false
            show500Btn.isVisible = false
            return
        }
        val needsPagination = total > PAGE_SIZE
        if (!showAll && needsPagination) {
            paginationLabel.text = "  Showing first $PAGE_SIZE of $total rows"
            showAllBtn.isVisible = true
            show500Btn.isVisible = false
        } else {
            paginationLabel.text = "  Showing all $total rows"
            showAllBtn.isVisible = false
            show500Btn.isVisible = showAll && needsPagination // allow switching back
        }
    }

    // ─── Search / filter ──────────────────────────────────────────────────────

    private fun applyFilter() {
        val text = searchField.text.trim()
        rowSorter.rowFilter = when {
            text.isBlank() -> null
            else -> try {
                RowFilter.regexFilter<DefaultTableModel, Int>("(?i)${Regex.escape(text)}")
            } catch (_: Exception) { null }
        }
    }

    // ─── Column sizing ────────────────────────────────────────────────────────

    /** Measures column widths from [allItems] (up to 30 samples) — independent of pagination. */
    private fun autoSizeColumns() {
        if (columnNames.isEmpty()) return
        val headerFm = table.tableHeader.getFontMetrics(table.tableHeader.font)
        val cellFm   = table.getFontMetrics(table.font)
        columnNames.forEachIndexed { i, colName ->
            val col     = table.columnModel.getColumn(i)
            val headerW = headerFm.stringWidth(colName) + JBUI.scale(24)
            val contentW = allItems.take(30)
                .mapNotNull { item ->
                    if (!item.isObject) null
                    else {
                        val node = item.get(colName)
                        when {
                            node == null || node.isNull -> "null"
                            node.isTextual             -> node.asText()
                            else                       -> node.toString()
                        }
                    }
                }
                .maxOfOrNull { s ->
                    val display = if (colName == "_class") s.substringAfterLast('.') else s
                    cellFm.stringWidth(display) + JBUI.scale(20)
                } ?: JBUI.scale(60)
            col.minWidth       = JBUI.scale(40)
            col.preferredWidth = maxOf(headerW, contentW).coerceAtMost(JBUI.scale(280))
        }
    }

    // ─── Toolbar ──────────────────────────────────────────────────────────────

    private fun buildToolbar(): JPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)

        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            isOpaque = false
            copyRowBtn = JButton("Copy Row", AllIcons.Actions.Copy).apply {
                toolTipText = "Copy selected row as a JSON object"
                addActionListener { copySelectedRow(); showButtonFeedback(copyRowBtn) }
            }
            copyJsonBtn = JButton("Copy JSON", AllIcons.FileTypes.Json).apply {
                toolTipText = "Copy all results as formatted JSON"
                addActionListener { copyAllAsJson(); showButtonFeedback(copyJsonBtn) }
            }
            copyStringBtn = JButton("Copy as String", AllIcons.FileTypes.Text).apply {
                toolTipText = "Copy all results as an escaped JSON string"
                addActionListener { copyAllAsString(); showButtonFeedback(copyStringBtn) }
            }
            add(copyRowBtn)
            add(copyJsonBtn)
            add(copyStringBtn)
        }

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2)).apply {
            isOpaque = false
            add(JLabel("Filter:").apply { foreground = JBColor.GRAY })
            add(searchField)
        }

        add(leftPanel, BorderLayout.WEST)
        add(rightPanel, BorderLayout.EAST)
    }

    // ─── Bottom bar ───────────────────────────────────────────────────────────

    private fun buildBottomBar(): JPanel {
        showAllBtn = JButton("Show All").apply {
            font = font.deriveFont(font.size - 1f)
            isVisible = false
            toolTipText = "Load and display all rows (may be slow for very large results)"
            addActionListener {
                showAll = true
                repopulateRows()
                applyFilter()
            }
        }
        show500Btn = JButton("Show $PAGE_SIZE").apply {
            font = font.deriveFont(font.size - 1f)
            isVisible = false
            toolTipText = "Return to showing only the first $PAGE_SIZE rows"
            addActionListener {
                showAll = false
                repopulateRows()
                applyFilter()
            }
        }

        val paginationRow = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(paginationLabel, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                isOpaque = false
                add(show500Btn)
                add(showAllBtn)
            }, BorderLayout.EAST)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
            add(paginationRow, BorderLayout.NORTH)
            add(statusLabel,   BorderLayout.SOUTH)
        }
    }

    // ─── Context menu ─────────────────────────────────────────────────────────

    private fun buildContextMenu(): JPopupMenu = JPopupMenu().apply {
        add(JMenuItem("Copy Cell Value", AllIcons.Actions.Copy).apply {
            addActionListener { copySelectedCell(); showStatusFeedback() }
        })
        add(JMenuItem("Copy Row as JSON").apply {
            addActionListener { copySelectedRow(); showStatusFeedback() }
        })
        add(JSeparator())
        add(JMenuItem("Copy All as JSON", AllIcons.FileTypes.Json).apply {
            addActionListener { copyAllAsJson(); showStatusFeedback() }
        })
        add(JMenuItem("Copy All as String", AllIcons.FileTypes.Text).apply {
            addActionListener { copyAllAsString(); showStatusFeedback() }
        })
        add(JSeparator())
        add(JMenuItem("Navigate to Class", AllIcons.Actions.Find).apply {
            addActionListener {
                val row = table.selectedRow
                val col = table.selectedColumn
                if (row < 0 || col < 0) return@addActionListener
                val colName = table.columnModel.getColumn(col).headerValue as? String
                if (colName != "_class") return@addActionListener
                val modelRow = table.convertRowIndexToModel(row)
                (tableModel.getValueAt(modelRow, col) as? String)?.let { navigateToClass(it) }
            }
        })
    }

    // ─── Mouse / selection handlers ───────────────────────────────────────────

    private fun preselectCell(e: MouseEvent) {
        val row = table.rowAtPoint(e.point)
        val col = table.columnAtPoint(e.point)
        if (row >= 0) table.setRowSelectionInterval(row, row)
        if (col >= 0) table.setColumnSelectionInterval(col, col)
    }

    private fun onDoubleClick(e: MouseEvent) {
        val row = table.rowAtPoint(e.point)
        val col = table.columnAtPoint(e.point)
        if (row < 0 || col < 0) return
        val colName = table.columnModel.getColumn(col).headerValue as? String ?: return
        if (colName != "_class") return
        val modelRow = table.convertRowIndexToModel(row)
        (tableModel.getValueAt(modelRow, col) as? String)?.let { navigateToClass(it) }
    }

    private fun updateCursor(e: MouseEvent) {
        val col     = table.columnAtPoint(e.point)
        val colName = if (col >= 0) table.columnModel.getColumn(col).headerValue as? String else null
        table.cursor = if (colName == "_class") Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                       else Cursor.getDefaultCursor()
    }

    private fun updateStatusLabel() {
        val row = table.selectedRow
        val col = table.selectedColumn
        if (row < 0 || col < 0) { statusLabel.text = " "; return }
        val colName = table.columnModel.getColumn(col).headerValue as? String
        if (colName == "_class") {
            val modelRow = table.convertRowIndexToModel(row)
            val fqn = tableModel.getValueAt(modelRow, col) as? String ?: ""
            statusLabel.text = if (fqn.isNotBlank()) "  $fqn" else " "
        } else {
            statusLabel.text = " "
        }
    }

    // ─── Copy actions ─────────────────────────────────────────────────────────

    private fun copySelectedCell() {
        val row = table.selectedRow
        val col = table.selectedColumn
        if (row < 0 || col < 0) return
        toClipboard(table.getValueAt(row, col)?.toString() ?: "")
    }

    private fun copySelectedRow() {
        val row = table.selectedRow
        if (row < 0) return
        val modelRow = table.convertRowIndexToModel(row)
        val map = linkedMapOf<String, Any?>()
        for (c in 0 until tableModel.columnCount) {
            map[tableModel.getColumnName(c)] = tableModel.getValueAt(modelRow, c)
        }
        toClipboard(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map))
    }

    private fun copyAllAsJson() {
        val formatted = try {
            mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readTree(lastRawJson))
                .replace("\r\n", "\n").replace("\r", "\n")
        } catch (_: Exception) { lastRawJson }
        toClipboard(formatted)
    }

    private fun copyAllAsString() {
        val compact = try {
            mapper.writeValueAsString(mapper.readTree(lastRawJson))
        } catch (_: Exception) { lastRawJson }
        val escaped = compact.replace("\\", "\\\\").replace("\"", "\\\"")
        toClipboard("\"$escaped\"")
    }

    // ─── Copy feedback ────────────────────────────────────────────────────────

    /**
     * Temporarily changes a toolbar [button] to a green "Copied!" state for
     * 1.5 s, then restores the original text/icon.
     */
    private fun showButtonFeedback(button: JButton) {
        buttonFeedbackTimer?.stop()
        val origText = button.text
        val origIcon = button.icon
        button.text      = "Copied!"
        button.icon      = AllIcons.General.InspectionsOK
        button.foreground = JBColor(Color(0x2E7D32), Color(0x66BB6A))
        button.isEnabled  = false
        buttonFeedbackTimer = Timer(1500) {
            button.text      = origText
            button.icon      = origIcon
            button.foreground = JBColor.foreground()
            button.isEnabled  = true
        }.also { it.isRepeats = false; it.start() }
    }

    /**
     * Flashes "✓ Copied to clipboard" in the status label for 1.5 s —
     * used by context-menu copy actions that have no dedicated button.
     */
    private fun showStatusFeedback() {
        statusFeedbackTimer?.stop()
        val origText  = statusLabel.text
        val origColor = statusLabel.foreground
        statusLabel.text       = "  ✓ Copied to clipboard"
        statusLabel.foreground  = JBColor(Color(0x2E7D32), Color(0x66BB6A))
        statusFeedbackTimer = Timer(1500) {
            statusLabel.text       = origText
            statusLabel.foreground  = origColor
        }.also { it.isRepeats = false; it.start() }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun navigateToClass(fqn: String) {
        val shortName = fqn.trim().substringAfterLast('.')
        val scope = GlobalSearchScope.projectScope(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            val vf: VirtualFile? = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
                FilenameIndex.getVirtualFilesByName("$shortName.java", scope).firstOrNull()
                    ?: FilenameIndex.getVirtualFilesByName("$shortName.kt", scope).firstOrNull()
            }
            vf?.let {
                ApplicationManager.getApplication().invokeLater {
                    OpenFileDescriptor(project, it).navigate(true)
                }
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun toClipboard(text: String) =
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)

    // ─── Cell renderer ────────────────────────────────────────────────────────

    private inner class ClassAwareCellRenderer : DefaultTableCellRenderer() {

        init { horizontalAlignment = SwingConstants.LEFT }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            val colName = table.columnModel.getColumn(column).headerValue as? String
            font = font.deriveFont(Font.PLAIN)
            when {
                value == null -> {
                    text = "null"
                    font = font.deriveFont(Font.ITALIC)
                    if (!isSelected) foreground = JBColor.GRAY
                }
                colName == "_class" && value is String && value.isNotBlank() -> {
                    if (!isSelected) foreground = JBColor(Color(0x0066CC), Color(0x4A9EFF))
                    // Display only the short name; full FQN via tooltip + status bar
                    val shortName = value.substringAfterLast('.')
                    text = "<html><u>${esc(shortName)}</u></html>"
                }
                else -> text = value.toString()
            }
            return this
        }

        private fun esc(s: String) =
            s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
