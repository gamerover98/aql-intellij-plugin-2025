package com.arangodb.intellij.aql.ui.console

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

/**
 * Editable table of AQL bind parameters extracted live from the query editor.
 *
 * AQL bind-parameter syntax:
 *  - `@name`  → value parameter   → driver key `name`
 *  - `@@name` → collection param  → driver key `@name`
 *
 * The **Parameter** column shows the syntax as it appears in the query
 * (`@surname`, `@@collection`). The **Value** column is user-editable.
 * Existing values survive a re-parse as long as the parameter name is still
 * present in the query; new parameters receive an empty value.
 *
 * Call [syncWithQuery] on every editor document change.
 * Call [getBindVars] at execution time — only non-blank values are returned
 * so that ArangoDB emits a clear error for any missing required parameter.
 */
class BindParamsPanel {

    private val model = ParamsTableModel()

    private val table = JBTable(model).apply {
        columnModel.getColumn(0).preferredWidth = 160
        columnModel.getColumn(1).preferredWidth = 340
        // Commit any in-progress cell edit when the table loses focus (e.g. click Execute)
        putClientProperty("terminateEditOnFocusLost", true)
    }

    val component: JComponent = JPanel(BorderLayout()).apply {
        add(JBScrollPane(table), BorderLayout.CENTER)
        border = JBUI.Borders.empty(4)
    }

    /** True if the last [syncWithQuery] found at least one bind parameter. */
    fun hasParams(): Boolean = model.rowCount > 0

    /** Number of bind parameters currently tracked. */
    fun paramCount(): Int = model.rowCount

    /**
     * Re-parses [query] for bind parameters and updates the table rows.
     * New parameters receive an empty value; preserved values survive round-trips.
     */
    fun syncWithQuery(query: String) {
        model.syncParams(extractParams(query))
    }

    /**
     * Returns the bind-variable map ready to pass to [com.arangodb.intellij.aql.actions.AqlDataService].
     * Entries with a blank value are excluded so that ArangoDB can report them as missing.
     */
    fun getBindVars(): Map<String, String> = model.getBindVars()

    // ─── Param extraction ─────────────────────────────────────────────────────

    /**
     * Returns an ordered, deduplicated list of (displayLabel → driverKey) pairs.
     *
     * String literals (`'…'` and `"…"`) are stripped before scanning to avoid
     * false-positives like `FILTER x.email == "user@example.com"`.
     *
     * `@@collection` → display `@@collection`, key `@collection`
     * `@surname`     → display `@surname`,     key `surname`
     */
    private fun extractParams(query: String): List<Pair<String, String>> {
        // Strip single- and double-quoted string literals
        val stripped = query.replace(Regex("""'[^']*'|"[^"]*""""), "\"\"")
        val seen = linkedSetOf<Pair<String, String>>()
        for (match in Regex("""@@(\w+)|@(\w+)""").findAll(stripped)) {
            val pair = if (match.groupValues[1].isNotEmpty())
                "@@${match.groupValues[1]}" to "@${match.groupValues[1]}"
            else
                "@${match.groupValues[2]}"  to match.groupValues[2]
            seen.add(pair)
        }
        return seen.toList()
    }

    // ─── Table model ──────────────────────────────────────────────────────────

    private class ParamsTableModel : AbstractTableModel() {

        // (displayLabel, driverKey, value)
        private val rows = mutableListOf<Triple<String, String, String>>()

        override fun getColumnCount() = 2
        override fun getRowCount()    = rows.size
        override fun getColumnName(col: Int) = if (col == 0) "Parameter" else "Value"
        override fun getColumnClass(col: Int) = String::class.java
        override fun isCellEditable(row: Int, col: Int) = col == 1

        override fun getValueAt(row: Int, col: Int): Any =
            if (col == 0) rows[row].first else rows[row].third

        override fun setValueAt(value: Any?, row: Int, col: Int) {
            if (col == 1 && row < rows.size)
                rows[row] = rows[row].copy(third = value?.toString() ?: "")
        }

        fun syncParams(params: List<Pair<String, String>>) {
            val existing = rows.associate { it.second to it.third }
            rows.clear()
            for ((display, key) in params)
                rows.add(Triple(display, key, existing[key] ?: ""))
            fireTableDataChanged()
        }

        fun getBindVars(): Map<String, String> =
            rows.filter { it.third.isNotBlank() }
                .associate { it.second to it.third }
    }
}
