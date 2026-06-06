package com.arangodb.intellij.aql.toolWindow

import com.arangodb.intellij.aql.ArangoBundle
import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.services.ArangoProjectService
import com.arangodb.intellij.aql.ui.DataWindowState
import com.arangodb.intellij.aql.ui.actions.*
import com.arangodb.intellij.aql.ui.renderers.AqlNodeModel
import com.arangodb.intellij.aql.ui.renderers.AqlNodeRenderer
import com.arangodb.intellij.aql.ui.windows.AqlConsoleWindow
import com.arangodb.intellij.aql.util.Icons
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Tool window for managing ArangoDB servers and databases within the IDE.
 *
 * Sprint 1 features:
 *  - A1: Status bar — ● Connected / ✕ Error / (no config)
 *  - A4: System collections in a collapsible "System (N)" folder
 *  - A5: Document counts loaded asynchronously for the active database
 *  - A7: Friendly empty-state message when no connection is configured
 *
 * Sprint 2 features:
 *  - B1: Right-click context menu (per node type)
 *  - B2: Edit and Remove server buttons in the toolbar
 *  - B3: Live filter field above the tree
 *  - B4: "System Collections" toggle button to show/hide the system folder
 *  - B6: Ctrl+C copies the selected node's name to the clipboard
 */
class ServerToolWindow(private val project: Project) : Disposable {

    companion object {
        const val WINDOW_ID = "ArangoToolWindow"
    }

    // ─── Tree ─────────────────────────────────────────────────────────────────

    private val schemaTree = Tree()

    // ─── B3: Search / filter ──────────────────────────────────────────────────

    private val searchField = JBTextField().apply {
        emptyText.text = "Filter collections, graphs, views…"
        toolTipText = "Filter tree nodes (case-insensitive)"
    }

    /** Snapshot of the full unfiltered model; used to rebuild when filter changes. */
    private var fullTreeModel: DefaultTreeModel? = null

    // ─── B4: System collections toggle ────────────────────────────────────────

    private var showSystemCollections = true

    // ─── A1: Status bar ───────────────────────────────────────────────────────

    private val statusLabel = JLabel(" ").apply {
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
            JBUI.Borders.empty(3, 8)
        )
        font = font.deriveFont(Font.PLAIN, (font.size - 1).toFloat())
        foreground = JBColor.GRAY
        horizontalAlignment = SwingConstants.LEFT
    }

    // ─── Layout ───────────────────────────────────────────────────────────────

    private val schemePanel = JPanel(BorderLayout())
    private val panel: JPanel

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        setupTree()

        // B2: Edit + Remove added to the toolbar alongside existing actions
        val decoratorPanel = ToolbarDecorator
            .createDecorator(schemaTree)
            .apply {
                setPanelBorder(BorderFactory.createEmptyBorder())
                setToolbarPosition(com.intellij.openapi.actionSystem.ActionToolbarPosition.TOP)
                addExtraAction(AddServerAction(project))
                addExtraAction(EditServerAction(project))
                addExtraAction(RemoveServerAction(project))
                addExtraAction(RefreshSchemeAction(project))
                addExtraAction(ExpandAllAction(schemaTree))
                addExtraAction(CollapseAllAction(schemaTree))
                addExtraAction(SetActiveAction(project))
                // B4: Toggle system collections visibility
                addExtraAction(object : ToggleAction(
                    "System Collections",
                    "Show or hide the system collections folder",
                    AllIcons.General.Filter
                ) {
                    override fun isSelected(e: AnActionEvent): Boolean = showSystemCollections
                    override fun setSelected(e: AnActionEvent, state: Boolean) {
                        showSystemCollections = state
                        fillTree()
                    }
                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                })
            }.createPanel()

        // B3: search field above the decorator (below the tree's own toolbar)
        val searchPanel = buildSearchPanel()

        val innerPanel = JPanel(BorderLayout()).apply {
            add(searchPanel,    BorderLayout.NORTH)
            add(decoratorPanel, BorderLayout.CENTER)
        }

        schemePanel.add(innerPanel,   BorderLayout.CENTER)
        schemePanel.add(statusLabel,  BorderLayout.SOUTH)

        val service = project.service<ArangoProjectService>()

        service
            .subscribe(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME) {
                SwingUtilities.invokeLater { fillTree() }
            }
            .subscribe(ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET) {
                val selectedNodes = schemaTree.getSelectedNodes(CheckedTreeNode::class.java) { node ->
                    (node.userObject as? AqlNodeModel)?.type == AqlNodeModel.Type.DATABASE
                }
                when {
                    selectedNodes.isEmpty()  -> ArangoBundle.notifyWarning("selectNodeError")
                    selectedNodes.size > 1   -> ArangoBundle.notifyWarning("selectOnlyOneNodeError")
                    else -> {
                        val selectedNode = selectedNodes.first().userObject as AqlNodeModel
                        service.setActiveDatabase(selectedNode)
                    }
                }
            }

        fillTree()

        panel = panel {
            row {
                cell(schemePanel)
                    .align(Align.FILL)
                    .resizableColumn()
            }
        }
    }

    // ─── Tree setup ───────────────────────────────────────────────────────────

    private fun setupTree() {
        schemaTree.cellRenderer = AqlNodeRenderer()
        schemaTree.isRootVisible = true
        schemaTree.showsRootHandles = true

        // A7: shown when tree has no visible rows (no config, or empty filter result)
        schemaTree.emptyText.setText("No ArangoDB connection configured")
        schemaTree.emptyText.appendText(
            "  —  click  +  to add a server",
            SimpleTextAttributes.GRAYED_ATTRIBUTES
        )

        // Mouse: double-click (existing) + right-click context menu (B1)
        schemaTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) onDoubleClick(e)
            }
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) onRightClick(e)
            }
        })

        // B6: Ctrl+C copies the selected node's display name
        schemaTree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_C &&
                    (e.modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0) {
                    val path = schemaTree.selectionPath ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                    val model = node.userObject as? AqlNodeModel ?: return
                    val name = (model.displayName?.takeIf { it.isNotBlank() } ?: model.name)
                        ?: return
                    toClipboard(name)
                }
            }
        })
    }

    // ─── B3: Search panel ─────────────────────────────────────────────────────

    private fun buildSearchPanel(): JPanel {
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter()
            override fun removeUpdate(e: DocumentEvent) = applyFilter()
            override fun changedUpdate(e: DocumentEvent) = applyFilter()
        })

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(2, 4)
            )
            add(JLabel(AllIcons.Actions.Find).apply {
                border = JBUI.Borders.empty(0, 2, 0, 4)
            }, BorderLayout.WEST)
            add(searchField, BorderLayout.CENTER)
        }
    }

    /**
     * Rebuilds the tree model to show only nodes whose [AqlNodeModel.displayName]
     * (case-insensitive) contains the current search text.
     * Container nodes (SERVER, DATABASE, CATEGORY) are kept whenever they have
     * at least one matching descendant.
     * When the filter is blank the full model is restored.
     */
    private fun applyFilter() {
        val model = fullTreeModel ?: return
        val text  = searchField.text.trim().lowercase()

        if (text.isBlank()) {
            schemaTree.model = model
            return
        }

        val filteredRoot = filterNode(model.root as? DefaultMutableTreeNode ?: return, text)
        schemaTree.model = if (filteredRoot != null) DefaultTreeModel(filteredRoot)
                           else DefaultTreeModel(DefaultMutableTreeNode())

        // Expand all rows so matches are immediately visible
        var i = 0
        while (i < schemaTree.rowCount) schemaTree.expandRow(i++)
    }

    /**
     * Returns a shallow copy of [source] (same [AqlNodeModel] reference) that
     * contains only children matching [filter], or `null` if neither the node
     * itself nor any descendant matches.
     */
    private fun filterNode(source: DefaultMutableTreeNode, filter: String): DefaultMutableTreeNode? {
        val model       = source.userObject as? AqlNodeModel
        val isContainer = model?.type?.let {
            it == AqlNodeModel.Type.SERVER ||
            it == AqlNodeModel.Type.DATABASE ||
            it == AqlNodeModel.Type.CATEGORY
        } ?: false

        val matchingChildren = (0 until source.childCount).mapNotNull { i ->
            filterNode(source.getChildAt(i) as? DefaultMutableTreeNode ?: return@mapNotNull null, filter)
        }

        return when {
            isContainer -> {
                if (matchingChildren.isEmpty()) null
                else DefaultMutableTreeNode(source.userObject).also { copy ->
                    matchingChildren.forEach { copy.add(it) }
                }
            }
            else -> {
                val name = (model?.displayName ?: model?.name ?: "").lowercase()
                if (!name.contains(filter)) null
                else DefaultMutableTreeNode(source.userObject).also { copy ->
                    matchingChildren.forEach { copy.add(it) }
                }
            }
        }
    }

    // ─── Tree population ──────────────────────────────────────────────────────

    private fun fillTree() {
        val service = AqlDataService.with(project)

        if (!service.hasValidSettings()) {
            schemaTree.isRootVisible = false
            schemaTree.model = DefaultTreeModel(DefaultMutableTreeNode())
            fullTreeModel = null
            setStatus(ConnectionState.NO_CONFIG)
            return
        }

        val server    = service.server()
        val treeModel = service.populateTree(server, showSystemCollections)

        fullTreeModel = treeModel
        schemaTree.isRootVisible = true
        applyFilter()   // respect any active search text

        if (server.databases.isNotEmpty()) {
            val state = project.getService(DataWindowState::class.java).state
            setStatus(ConnectionState.CONNECTED, "${state.host}:${state.port}")
            loadCountsAsync(treeModel)
        } else {
            setStatus(ConnectionState.ERROR)
        }
    }

    // ─── B1: Context menu ─────────────────────────────────────────────────────

    private fun onRightClick(e: MouseEvent) {
        val path = schemaTree.getPathForLocation(e.x, e.y) ?: return
        schemaTree.selectionPath = path
        val node  = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val model = node.userObject as? AqlNodeModel ?: return
        buildContextMenu(model)?.show(schemaTree, e.x, e.y)
    }

    private fun buildContextMenu(model: AqlNodeModel): JPopupMenu? {
        val menu = JPopupMenu()
        when (model.type) {
            AqlNodeModel.Type.SERVER -> {
                menu.add(JMenuItem("Edit Server", Icons.ICON_EDIT).apply {
                    addActionListener { AqlDataService.with(project).showServerDialog() }
                })
                menu.add(JMenuItem("Remove Server", Icons.ICON_DELETE).apply {
                    addActionListener { confirmAndRemoveServer() }
                })
                menu.addSeparator()
                menu.add(JMenuItem("Refresh", AllIcons.Actions.Refresh).apply {
                    addActionListener { AqlDataService.with(project).refreshSchema() }
                })
                menu.addSeparator()
                menu.add(JMenuItem("Copy Address", AllIcons.Actions.Copy).apply {
                    val state = project.getService(DataWindowState::class.java).state
                    addActionListener { toClipboard("${state.host}:${state.port}") }
                })
            }
            AqlNodeModel.Type.DATABASE -> {
                menu.add(JMenuItem("Set Active", Icons.ICON_SELECTED).apply {
                    addActionListener { AqlDataService.with(project).setActiveDatabase(model) }
                })
                menu.addSeparator()
                menu.add(JMenuItem("Open in Console", AllIcons.Actions.Execute).apply {
                    addActionListener { openInConsole() }
                })
                menu.add(JMenuItem("Copy Name", AllIcons.Actions.Copy).apply {
                    addActionListener { toClipboard(model.displayName ?: "") }
                })
            }
            AqlNodeModel.Type.COLLECTION, AqlNodeModel.Type.EDGE -> {
                val colName = model.displayName ?: ""
                menu.add(JMenuItem("Execute Sample Query", AllIcons.Actions.Execute).apply {
                    addActionListener { executeSampleQuery(colName) }
                })
                menu.addSeparator()
                menu.add(JMenuItem("Copy Name", AllIcons.Actions.Copy).apply {
                    addActionListener { toClipboard(colName) }
                })
                menu.add(JMenuItem("Copy as AQL Identifier").apply {
                    toolTipText = "Copies the name wrapped in backticks, ready to paste in an AQL query"
                    addActionListener { toClipboard("`$colName`") }
                })
            }
            AqlNodeModel.Type.GRAPH, AqlNodeModel.Type.VIEW -> {
                menu.add(JMenuItem("Copy Name", AllIcons.Actions.Copy).apply {
                    addActionListener { toClipboard(model.displayName ?: "") }
                })
                menu.add(JMenuItem("Open in Console", AllIcons.Actions.Execute).apply {
                    addActionListener { openInConsole() }
                })
            }
            else -> return null
        }
        return menu
    }

    // ─── Mouse actions ────────────────────────────────────────────────────────

    private fun onDoubleClick(e: MouseEvent) {
        val path = schemaTree.getPathForLocation(e.x, e.y) ?: return
        val node  = path.lastPathComponent as? CheckedTreeNode ?: return
        val model = node.userObject as? AqlNodeModel ?: return
        if (model.type != AqlNodeModel.Type.COLLECTION && model.type != AqlNodeModel.Type.EDGE) return
        executeSampleQuery(model.displayName ?: return)
    }

    private fun executeSampleQuery(collectionName: String) {
        AqlDataService.with(project)
            .executeQuery("FOR doc IN `$collectionName` LIMIT 100 RETURN doc")
        openInConsole()
    }

    private fun openInConsole() {
        ToolWindowManager.getInstance(project)
            .getToolWindow(AqlConsoleWindow.WINDOW_ID)
            ?.activate(null, true)
    }

    private fun confirmAndRemoveServer() {
        val choice = Messages.showYesNoDialog(
            project,
            "Remove the current server configuration?\nThis action cannot be undone.",
            "Remove Server",
            Messages.getQuestionIcon()
        )
        if (choice == Messages.YES) AqlDataService.with(project).removeServer()
    }

    // ─── A1: Status bar ───────────────────────────────────────────────────────

    private enum class ConnectionState { CONNECTED, ERROR, NO_CONFIG }

    private fun setStatus(state: ConnectionState, detail: String? = null) {
        when (state) {
            ConnectionState.NO_CONFIG -> {
                statusLabel.text = " "
                statusLabel.foreground = JBColor.GRAY
            }
            ConnectionState.CONNECTED -> {
                val suffix = if (!detail.isNullOrEmpty()) "  —  $detail" else ""
                statusLabel.text = "  ● Connected$suffix"
                statusLabel.foreground = JBColor(Color(0x2E7D32), Color(0x66BB6A))
            }
            ConnectionState.ERROR -> {
                statusLabel.text = "  ✕ Connection failed — check settings"
                statusLabel.foreground = JBColor(Color(0xC62828), Color(0xEF5350))
            }
        }
    }

    // ─── A5: Async document-count loading ─────────────────────────────────────

    /**
     * Off the EDT: fetches document counts for every COLLECTION/EDGE node in the
     * active database and calls [DefaultTreeModel.nodeChanged] on the EDT so the
     * renderer repaints individual rows without a full tree rebuild.
     */
    private fun loadCountsAsync(treeModel: DefaultTreeModel) {
        val service = AqlDataService.with(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            val root = treeModel.root as? DefaultMutableTreeNode ?: return@executeOnPooledThread
            for (i in 0 until root.childCount) {
                val dbNode  = root.getChildAt(i) as? DefaultMutableTreeNode ?: continue
                val dbModel = dbNode.userObject as? AqlNodeModel ?: continue
                if (!dbModel.isSelected) continue

                for (j in 0 until dbNode.childCount) {
                    val colNode  = dbNode.getChildAt(j) as? DefaultMutableTreeNode ?: continue
                    val colModel = colNode.userObject as? AqlNodeModel ?: continue
                    if (colModel.type != AqlNodeModel.Type.COLLECTION &&
                        colModel.type != AqlNodeModel.Type.EDGE) continue
                    val colName = colModel.displayName ?: continue

                    val count = try { service.getCollectionCount(colName) } catch (_: Exception) { -1L }
                    if (count >= 0) {
                        colModel.count = count
                        SwingUtilities.invokeLater { treeModel.nodeChanged(colNode) }
                    }
                }
                break
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun toClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun getContent(): JPanel = panel
    fun getProject(): Project = project

    override fun dispose() { /* nothing */ }
}
