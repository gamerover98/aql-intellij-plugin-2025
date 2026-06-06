package com.arangodb.intellij.aql.ui.console

import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.lang.AqlLanguage
import com.arangodb.intellij.aql.services.AqlConsoleStateService
import com.arangodb.intellij.aql.ui.DataWindowState
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
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
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.swing.*

/**
 * AQL Console as an IntelliJ editor tab — editor-only half.
 *
 * Contains:
 *  - A [LanguageTextField] for composing AQL queries (with AQL syntax highlighting)
 *  - A toolbar: DB selector, Execute, Explain, Clear buttons
 *  - A Query History panel
 *
 * When Execute is clicked a new [AqlResultVirtualFile] / [AqlResultFileEditor] tab is
 * opened for that execution. Multiple result tabs can coexist simultaneously.
 *
 * Replaces the old [com.arangodb.intellij.aql.ui.windows.AqlConsoleWindow].
 * State (editor text, history) is persisted via [AqlConsoleStateService].
 */
class AqlConsoleFileEditor(
    private val project: Project,
    private val virtualFile: VirtualFile
) : FileEditor {

    companion object {
        private val TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
    }

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

    // ─── Editor area ──────────────────────────────────────────────────────────
    private val editorField = LanguageTextField(AqlLanguage, project, "", false)
    private val editorTextListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            AqlConsoleStateService.getInstance(project).state.editorText = editorField.text
        }
    }

    // ─── Toolbar ──────────────────────────────────────────────────────────────
    private val dbSelector = JComboBox<String>()
    private var isUpdatingDbSelector = false
    private var executeBtn: JButton? = null
    private var explainBtn: JButton? = null

    // ─── History ──────────────────────────────────────────────────────────────
    private val historyModel = DefaultListModel<HistoryEntry>()
    private val historyList  = JBList(historyModel)
    private var isRestoringHistory = false

    // ─── Root component ───────────────────────────────────────────────────────
    private val root: JComponent

    // ─── Message bus connection ───────────────────────────────────────────────
    private val busConnection = project.messageBus.connect()

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

    // ─── UI construction ──────────────────────────────────────────────────────

    private fun buildUI(): JComponent {
        val toolbar = buildToolbar()

        val editorWrapper = JPanel(BorderLayout()).apply {
            add(toolbar, BorderLayout.NORTH)
            add(editorField, BorderLayout.CENTER)
            border = JBUI.Borders.empty()
        }

        val historyPane = JBTabbedPane().apply {
            addTab("Query History", AllIcons.Vcs.History, buildHistoryPanel())
        }

        val splitter = JBSplitter(true, 0.65f).apply {
            firstComponent  = editorWrapper
            secondComponent = historyPane
            border = JBUI.Borders.empty()
        }

        return JPanel(BorderLayout()).apply { add(splitter, BorderLayout.CENTER) }
    }

    private fun buildToolbar(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            border = JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 0, 0, 1, 0)
        }

        dbSelector.preferredSize = JBUI.size(160, 24)

        executeBtn = JButton("Execute", AllIcons.Actions.Execute).apply {
            toolTipText = "Execute query (Ctrl+Enter)"
            addActionListener { executeQuery() }
        }
        explainBtn = JButton("Explain", AllIcons.Actions.Preview).apply {
            toolTipText = "Explain query"
            addActionListener { explainQuery() }
        }
        val clearBtn = JButton("Clear", AllIcons.Actions.GC).apply {
            toolTipText = "Clear editor"
            addActionListener { clearEditor() }
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
            if (e.valueIsAdjusting || isRestoringHistory) return@addListSelectionListener
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
        editorField.addDocumentListener(editorTextListener)

        // Add to history when a query result arrives (any query, not filtered by ID here)
        busConnection.subscribe(ActionBusEvent.AQL_QUERY_RESULT, ActionBusEvent { data ->
            val query = data.get(com.arangodb.intellij.aql.actions.ActionEventData.KEY_QUERY) ?: return@ActionBusEvent
            if (query.isBlank()) return@ActionBusEvent
            val ts = LocalDateTime.now().format(TS_FMT)
            SwingUtilities.invokeLater {
                historyList.clearSelection()
                historyModel.insertElementAt(HistoryEntry(ts, query), 0)
                if (historyModel.size > 200) historyModel.removeElementAt(historyModel.size - 1)
                val consoleState = AqlConsoleStateService.getInstance(project).state
                consoleState.history.clear()
                consoleState.history.addAll(
                    (0 until historyModel.size).map { i ->
                        val e = historyModel.getElementAt(i)
                        AqlConsoleStateService.HistoryItem(e.timestamp, e.query)
                    }
                )
            }
        })
        busConnection.subscribe(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME, ActionBusEvent { _ ->
            populateDatabaseSelector()
        })
        busConnection.subscribe(ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET, ActionBusEvent { _ ->
            populateDatabaseSelector()
        })
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private fun executeQuery() {
        val query = editorField.text.trim().ifEmpty { return }
        val queryId = UUID.randomUUID().toString()
        val resultFile = AqlResultVirtualFile("Result", queryId)
        FileEditorManager.getInstance(project).openFile(resultFile, true)
        setRunningState(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                AqlDataService.with(project).executeQuery(query, queryId)
            } finally {
                SwingUtilities.invokeLater { setRunningState(false) }
            }
        }
    }

    private fun explainQuery() {
        val query = editorField.text.trim().ifEmpty { return }
        val queryId = UUID.randomUUID().toString()
        val resultFile = AqlResultVirtualFile("Explain", queryId)
        FileEditorManager.getInstance(project).openFile(resultFile, true)
        setRunningState(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                AqlDataService.with(project).explainQuery(query, queryId)
            } finally {
                SwingUtilities.invokeLater { setRunningState(false) }
            }
        }
    }

    private fun clearEditor() {
        editorField.text = ""
        AqlConsoleStateService.getInstance(project).state.editorText = ""
    }

    /** Disables/enables the Execute and Explain buttons while a query is in flight. */
    private fun setRunningState(running: Boolean) {
        executeBtn?.isEnabled = !running
        explainBtn?.isEnabled = !running
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
                        com.arangodb.intellij.aql.ui.renderers.AqlNodeModel(
                            db, db, com.arangodb.intellij.aql.ui.renderers.AqlNodeModel.Type.DATABASE
                        )
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

        if (state.editorText.isNotBlank()) {
            editorField.text = state.editorText
        }

        if (state.history.isNotEmpty()) {
            SwingUtilities.invokeLater {
                isRestoringHistory = true
                try {
                    state.history.forEach { item ->
                        historyModel.addElement(HistoryEntry(item.timestamp, item.query))
                    }
                } finally {
                    isRestoringHistory = false
                }
            }
        }
    }

    // ─── FileEditor interface ─────────────────────────────────────────────────

    override fun getComponent(): JComponent = root
    override fun getPreferredFocusedComponent(): JComponent = editorField
    override fun getName(): String = "AQL Console"
    override fun getFile(): VirtualFile = virtualFile
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = !project.isDisposed

    override fun dispose() {
        editorField.removeDocumentListener(editorTextListener)
        busConnection.disconnect()
    }
}
