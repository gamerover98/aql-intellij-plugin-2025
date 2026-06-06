package com.arangodb.intellij.aql.ui.windows

import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.lang.AqlLanguage
import com.arangodb.intellij.aql.services.AqlConsoleStateService
import com.arangodb.intellij.aql.services.AqlResultService
import com.arangodb.intellij.aql.ui.DataWindowState
import com.arangodb.intellij.aql.ui.panels.AqlGraphPanel
import com.arangodb.intellij.aql.ui.panels.ResultsPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBSplitter
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

class AqlConsoleWindow(private val project: Project, @Suppress("UNUSED_PARAMETER") toolWindow: ToolWindow) : Disposable {

    companion object {
        const val WINDOW_ID = "ArangoDB Console"
        private val TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
        /** Cap on the persisted raw-result size to avoid bloating the workspace XML. */
        private const val MAX_PERSISTED_RESULT_CHARS = 200_000
    }

    // ─── Editor area ────────────────────────────────────────────────────────
    private val editorField = LanguageTextField(AqlLanguage, project, "", false)
    private val editorTextListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            AqlConsoleStateService.getInstance(project).state.editorText = editorField.text
        }
    }

    // ─── Toolbar components ─────────────────────────────────────────────────
    private val dbSelector = JComboBox<String>()
    private var isUpdatingDbSelector = false

    // ─── Result panels ──────────────────────────────────────────────────────
    /** Table + JSON tree sub-tabs for query results. */
    private val resultsPanel = ResultsPanel(project)
    private val graphPanel   = AqlGraphPanel()

    // ─── History ────────────────────────────────────────────────────────────
    private val historyModel = DefaultListModel<HistoryEntry>()
    private val historyList  = JBList(historyModel)

    // ─── Root component ─────────────────────────────────────────────────────
    private val tabs = JBTabbedPane()
    private val root: JComponent

    private data class HistoryEntry(val timestamp: String, val query: String) {
        override fun toString() = "[$timestamp] ${query.lines().first().take(60)}"
    }

    init {
        root = buildUI()
        wireDbSelector()
        wireEvents()
        populateDatabaseSelector()
        restorePersistedState()
    }

    // ─── UI construction ────────────────────────────────────────────────────

    private fun buildUI(): JComponent {
        val toolbar = buildToolbar()

        val editorWrapper = JPanel(BorderLayout()).apply {
            add(toolbar, BorderLayout.NORTH)
            add(editorField, BorderLayout.CENTER)
            border = JBUI.Borders.empty()
        }

        // Outer result tabs: [0] JSON Results, [1] Graph View, [2] Query History
        tabs.addTab("JSON Results", AllIcons.FileTypes.Json,   resultsPanel.component)
        tabs.addTab("Graph View",   AllIcons.Nodes.Related,    graphPanel)
        tabs.addTab("Query History",AllIcons.Vcs.History,      buildHistoryPanel())

        val splitter = JBSplitter(true, 0.35f).apply {
            firstComponent  = editorWrapper
            secondComponent = tabs
            border = JBUI.Borders.empty()
        }

        return JPanel(BorderLayout()).apply { add(splitter, BorderLayout.CENTER) }
    }

    private fun buildToolbar(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            border = JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 0, 0, 1, 0)
        }

        dbSelector.preferredSize = JBUI.size(160, 24)

        val executeBtn = JButton("Execute", AllIcons.Actions.Execute).apply {
            toolTipText = "Execute query (Ctrl+Enter)"
            addActionListener { executeQuery() }
        }
        val explainBtn = JButton("Explain", AllIcons.Actions.Preview).apply {
            toolTipText = "Explain query"
            addActionListener { explainQuery() }
        }
        val clearBtn = JButton("Clear", AllIcons.Actions.GC).apply {
            toolTipText = "Clear editor and results"
            addActionListener { clearAll() }
        }

        panel.add(JBLabel("Database:"))
        panel.add(dbSelector)
        panel.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = JBUI.size(1, 20) })
        panel.add(executeBtn)
        panel.add(explainBtn)
        panel.add(clearBtn)

        // Ctrl+Enter in editor → execute
        editorField.addSettingsProvider { editor ->
            editor.contentComponent.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "aql.execute"
            )
            editor.contentComponent.actionMap.put("aql.execute", object : AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent) = executeQuery()
            })
        }

        return panel
    }

    private fun buildHistoryPanel(): JComponent {
        historyList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        historyList.addListSelectionListener { e ->
            if (e.valueIsAdjusting) return@addListSelectionListener
            val entry = historyList.selectedValue ?: return@addListSelectionListener
            editorField.text = entry.query
        }
        val clearHistoryBtn = JButton("Clear History").apply {
            addActionListener {
                historyModel.clear()
                AqlConsoleStateService.getInstance(project).state.history.clear()
            }
        }
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(historyList), BorderLayout.CENTER)
            add(clearHistoryBtn, BorderLayout.SOUTH)
        }
    }

    // ─── Event wiring ─────────────────────────────────────────────────────────

    private fun wireEvents() {
        // Persist editor content on every keystroke (in-memory; flushed on IDE close)
        editorField.addDocumentListener(editorTextListener)

        val bus = project.messageBus.connect(this)

        bus.subscribe(ActionBusEvent.AQL_QUERY_RESULT, ActionBusEvent { data ->
            processResult(data)
        })
        bus.subscribe(ActionBusEvent.AQL_SYSTEM_EMPTY_LOG, ActionBusEvent { _ ->
            SwingUtilities.invokeLater {
                resultsPanel.clear()
                graphPanel.clearData()
            }
        })
        bus.subscribe(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME, ActionBusEvent { _ ->
            populateDatabaseSelector()
        })
        bus.subscribe(ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET, ActionBusEvent { _ ->
            populateDatabaseSelector()
        })
    }

    private fun processResult(data: ActionEventData) {
        val raw   = data.get(ActionEventData.KEY_RESULT) ?: return
        val query = data.get(ActionEventData.KEY_QUERY)  ?: ""

        // Service side-effect: expose last result for copy/export
        project.getService(AqlResultService::class.java).lastResult = raw

        // Persist (capped to avoid bloating workspace XML)
        val consoleState = AqlConsoleStateService.getInstance(project).state
        consoleState.lastResult = raw.take(MAX_PERSISTED_RESULT_CHARS)

        // Persist history
        val ts = LocalDateTime.now().format(TS_FMT)
        if (query.isNotBlank()) {
            SwingUtilities.invokeLater {
                historyModel.insertElementAt(HistoryEntry(ts, query), 0)
                if (historyModel.size > 200) historyModel.removeElementAt(historyModel.size - 1)
                consoleState.history.clear()
                consoleState.history.addAll(
                    (0 until historyModel.size).map { i ->
                        val e = historyModel.getElementAt(i)
                        AqlConsoleStateService.HistoryItem(e.timestamp, e.query)
                    }
                )
            }
        }

        // All Swing updates on EDT
        SwingUtilities.invokeLater {
            resultsPanel.setData(raw)
            graphPanel.setData(raw)
            // Auto-switch outer tab: Graph View for edge data, JSON Results otherwise
            tabs.selectedIndex = if (raw.contains("\"_from\"") && raw.contains("\"_to\"")) 1 else 0
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private fun executeQuery() {
        val query = editorField.text.trim().ifEmpty { return }
        ApplicationManager.getApplication().executeOnPooledThread {
            AqlDataService.with(project).executeQuery(query)
        }
    }

    private fun explainQuery() {
        val query = editorField.text.trim().ifEmpty { return }
        ApplicationManager.getApplication().executeOnPooledThread {
            AqlDataService.with(project).explainQuery(query)
        }
    }

    private fun clearAll() {
        editorField.text = ""
        resultsPanel.clear()
        graphPanel.clearData()
        AqlConsoleStateService.getInstance(project).state.lastResult = ""
    }

    // ─── Database selector ────────────────────────────────────────────────────

    private fun wireDbSelector() {
        dbSelector.addActionListener {
            if (isUpdatingDbSelector) return@addActionListener
            val db = dbSelector.selectedItem as? String ?: return@addActionListener
            val currentDb = project.getService(DataWindowState::class.java).state.selectedDatabase?.name
            if (currentDb != db) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    AqlDataService.with(project).setActiveDatabase(
                        com.arangodb.intellij.aql.ui.renderers.AqlNodeModel(db, db, com.arangodb.intellij.aql.ui.renderers.AqlNodeModel.Type.DATABASE)
                    )
                }
            }
        }
    }

    private fun populateDatabaseSelector() {
        val state     = project.getService(DataWindowState::class.java).state
        val databases = state.databases.map { it.name ?: "" }.filter { it.isNotEmpty() }.sorted()
        val selected  = state.selectedDatabase?.name ?: state.selectedDatabaseName

        SwingUtilities.invokeLater {
            isUpdatingDbSelector = true
            try {
                val current = dbSelector.selectedItem as? String
                dbSelector.removeAllItems()
                databases.forEach { dbSelector.addItem(it) }
                when {
                    selected != null && databases.contains(selected) -> dbSelector.selectedItem = selected
                    current  != null && databases.contains(current)  -> dbSelector.selectedItem = current
                    databases.isNotEmpty()                           -> dbSelector.selectedIndex = 0
                }
            } finally {
                isUpdatingDbSelector = false
            }
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    private fun restorePersistedState() {
        val state = AqlConsoleStateService.getInstance(project).state

        // Restore editor text (EditorTextField buffers until the underlying editor is created)
        if (state.editorText.isNotBlank()) {
            editorField.text = state.editorText
        }

        // Restore history list (stored newest-first)
        if (state.history.isNotEmpty()) {
            SwingUtilities.invokeLater {
                state.history.forEach { item ->
                    historyModel.addElement(HistoryEntry(item.timestamp, item.query))
                }
            }
        }

        // Restore last result in the results panel
        if (state.lastResult.isNotBlank()) {
            SwingUtilities.invokeLater {
                resultsPanel.restoreResult(state.lastResult)
            }
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun getContent(): JComponent = root

    fun setQueryText(query: String) {
        editorField.text = query
    }

    override fun dispose() {
        editorField.removeDocumentListener(editorTextListener)
    }
}
