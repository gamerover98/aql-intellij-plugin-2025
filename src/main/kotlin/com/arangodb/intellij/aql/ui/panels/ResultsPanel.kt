package com.arangodb.intellij.aql.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

/**
 * Container panel for AQL query results — hosts three sub-tabs:
 *  - **Table**  : columnar view with copy actions and _class navigation ([ResultTablePanel])
 *  - **JSON**   : collapsible, color-coded tree ([JsonTreePanel])
 *  - **String** : compact escaped JSON string for pasting into code ([JsonStringPanel])
 *
 * All three sub-panels are updated together on every [setData] call.
 * The outer "JSON Results" tab in [AqlConsoleWindow] embeds [component].
 */
class ResultsPanel(project: Project) {

    private val tablePanel     = ResultTablePanel(project)
    private val jsonTreePanel  = JsonTreePanel(project)
    private val jsonStringPanel = JsonStringPanel()

    private val subTabs = JBTabbedPane().apply {
        tabComponentInsets = JBUI.insetsTop(0)
        addTab("Table",  AllIcons.Nodes.DataTables, tablePanel)
        addTab("JSON",   AllIcons.FileTypes.Json,   jsonTreePanel)
        addTab("String", AllIcons.FileTypes.Text,   jsonStringPanel)
    }

    /** The embeddable Swing component for this panel. */
    val component: JComponent = subTabs

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Populate all sub-panels from [raw] JSON returned by a query. */
    fun setData(raw: String) {
        tablePanel.setData(raw)
        jsonTreePanel.setData(raw)
        jsonStringPanel.setData(raw)
    }

    /** Clear all sub-panels (e.g. after "Clear" button). */
    fun clear() {
        tablePanel.clear()
        jsonTreePanel.clear()
        jsonStringPanel.clear()
    }

    /**
     * Restore persisted result on IDE startup — identical to [setData] but
     * semantically distinct (no side-effects like outer tab auto-switching).
     */
    fun restoreResult(raw: String) = setData(raw)
}
