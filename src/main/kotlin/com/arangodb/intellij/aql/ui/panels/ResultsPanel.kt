package com.arangodb.intellij.aql.ui.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

/**
 * Container panel for AQL query results — hosts two sub-tabs:
 *  - **Table** : columnar view ([ResultTablePanel])
 *  - **JSON**  : collapsible, color-coded tree ([JsonTreePanel])
 *
 * Both sub-panels are updated together on every [setData] call.
 * The outer "JSON Results" tab in [AqlConsoleWindow] embeds [component].
 */
class ResultsPanel(project: Project) {

    private val tablePanel    = ResultTablePanel(project)
    private val jsonTreePanel = JsonTreePanel(project)

    private val subTabs = JBTabbedPane().apply {
        tabComponentInsets = JBUI.insetsTop(0)
        addTab("Table", AllIcons.Nodes.DataTables,  tablePanel)
        addTab("JSON",  AllIcons.FileTypes.Json,     jsonTreePanel)
    }

    /** The embeddable Swing component for this panel. */
    val component: JComponent = subTabs

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Populate both sub-panels from [raw] JSON returned by a query. */
    fun setData(raw: String) {
        tablePanel.setData(raw)
        jsonTreePanel.setData(raw)
    }

    /** Clear both sub-panels (e.g. after "Clear" button). */
    fun clear() {
        tablePanel.clear()
        jsonTreePanel.clear()
    }

    /**
     * Restore persisted result on IDE startup — identical to [setData] but
     * semantically distinct (no side-effects like tab auto-switching).
     */
    fun restoreResult(raw: String) = setData(raw)
}
