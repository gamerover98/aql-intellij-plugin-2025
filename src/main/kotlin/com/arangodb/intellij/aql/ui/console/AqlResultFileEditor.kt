package com.arangodb.intellij.aql.ui.console

import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.services.AqlResultService
import com.arangodb.intellij.aql.ui.panels.AqlGraphPanel
import com.arangodb.intellij.aql.ui.panels.ResultsPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Editor tab that displays the result of a single AQL query execution.
 *
 * One tab is created per execution — multiple tabs can coexist simultaneously for
 * side-by-side comparison of different query results.
 *
 * The [queryId] links this editor to a specific execution: the editor subscribes to
 * [ActionBusEvent.AQL_QUERY_RESULT] and ignores events whose [ActionEventData.KEY_QUERY_ID]
 * does not match its own [queryId], preventing cross-contamination between tabs.
 *
 * Before the result arrives the tab shows a "⏳ Executing…" placeholder. Once the
 * result arrives via the message bus the placeholder is replaced by the results view.
 */
class AqlResultFileEditor(
    private val project: Project,
    private val virtualFile: VirtualFile,
    private val queryId: String
) : FileEditor {

    // ─── UserDataHolder delegation ─────────────────────────────────────────────
    private val userDataHolder = UserDataHolderBase()
    override fun <T : Any?> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = userDataHolder.putUserData(key, value)

    // ─── Property change support ───────────────────────────────────────────────
    private val pcs = PropertyChangeSupport(this)
    override fun addPropertyChangeListener(listener: PropertyChangeListener) =
        pcs.addPropertyChangeListener(listener)
    override fun removePropertyChangeListener(listener: PropertyChangeListener) =
        pcs.removePropertyChangeListener(listener)

    // ─── Result panels ─────────────────────────────────────────────────────────
    private val resultsPanel = ResultsPanel(project)
    private val graphPanel   = AqlGraphPanel()
    private val tabs         = JBTabbedPane()

    // ─── Loading placeholder ───────────────────────────────────────────────────
    private val loadingLabel = JBLabel(
        "⏳ Executing…",
        AllIcons.Process.Step_1,
        SwingConstants.CENTER
    )

    // ─── Root ──────────────────────────────────────────────────────────────────
    private val root: JPanel = JPanel(BorderLayout()).also { panel ->
        tabs.addTab("JSON Results", AllIcons.FileTypes.Json, resultsPanel.component)
        tabs.addTab("Graph View",   AllIcons.Nodes.Related,  graphPanel)
        panel.add(loadingLabel, BorderLayout.CENTER)
    }

    // ─── Message bus ───────────────────────────────────────────────────────────
    private val busConnection = project.messageBus.connect()

    init {
        busConnection.subscribe(ActionBusEvent.AQL_QUERY_RESULT, ActionBusEvent { data ->
            // Only handle the event that belongs to this tab's execution
            val id = data.get(ActionEventData.KEY_QUERY_ID) ?: return@ActionBusEvent
            if (id != queryId) return@ActionBusEvent
            showResult(data.get(ActionEventData.KEY_RESULT) ?: "")
        })
    }

    private fun showResult(raw: String) {
        SwingUtilities.invokeLater {
            root.remove(loadingLabel)
            root.add(tabs, BorderLayout.CENTER)
            root.revalidate()
            root.repaint()
            resultsPanel.setData(raw)
            graphPanel.setData(raw)
            // Auto-switch to Graph View when edge data is present
            tabs.selectedIndex = if (raw.contains("\"_from\"") && raw.contains("\"_to\"")) 1 else 0
            // Expose last result for copy/export actions
            project.getService(AqlResultService::class.java).lastResult = raw
        }
    }

    // ─── FileEditor interface ──────────────────────────────────────────────────

    override fun getComponent(): JComponent = root
    override fun getPreferredFocusedComponent(): JComponent = root
    override fun getName(): String = virtualFile.nameWithoutExtension
    override fun getFile(): VirtualFile = virtualFile
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = !project.isDisposed
    override fun dispose() { busConnection.disconnect() }
}
