package com.arangodb.intellij.aql.toolWindow

import com.arangodb.intellij.aql.ArangoBundle
import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.services.AqlConsoleStateService
import com.arangodb.intellij.aql.services.ArangoProjectService
import com.arangodb.intellij.aql.model.ArangoDbServer
import com.arangodb.intellij.aql.services.ServerListState
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
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
 *
 * Sprint 3 features:
 *  - A3: Virtual category folders (Collections, Edge Collections, Graphs, Views)
 *  - A6: Rich HTML hover tooltips on SERVER and DATABASE nodes
 *  - A8: "Last refreshed HH:mm:ss" appended to the connected status bar
 *  - B5: Tree expansion state persisted across IDE restarts via AqlConsoleStateService
 *  - B7: "Open in Console" on DATABASE sets that DB as active before switching tabs
 *
 * Sprint 4 features:
 *  - C1: Auto-refresh — configurable scheduler (5/15/30 min) started on successful connection
 *  - C2: Multi-server — ServerListState registry, invisible root, "Set Active" on SERVER nodes
 *  - C3: Collection detail panel — index table shown below the tree on single click
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

    // ─── A8: Last successful refresh timestamp ────────────────────────────────

    private var lastRefreshTime: LocalTime? = null

    // ─── C1: Auto-refresh scheduler ───────────────────────────────────────────

    private var autoRefreshScheduler: ScheduledExecutorService? = null

    // ─── C3: Collection detail panel ──────────────────────────────────────────

    private val collectionDetailPanel = CollectionDetailPanel()

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

        val treePanel = JPanel(BorderLayout()).apply {
            add(searchPanel,    BorderLayout.NORTH)
            add(decoratorPanel, BorderLayout.CENTER)
        }

        // C3: vertical split — tree on top, collection detail panel below
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, treePanel, collectionDetailPanel).apply {
            resizeWeight = 0.75
            dividerSize  = 4
            isContinuousLayout = true
        }

        schemePanel.add(splitPane,   BorderLayout.CENTER)
        schemePanel.add(statusLabel, BorderLayout.SOUTH)

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

        // C3: single-click on COLLECTION/EDGE → load and show collection details
        schemaTree.addTreeSelectionListener(object : TreeSelectionListener {
            override fun valueChanged(e: TreeSelectionEvent) {
                val path  = e.newLeadSelectionPath
                val node  = path?.lastPathComponent as? DefaultMutableTreeNode
                val model = node?.userObject as? AqlNodeModel
                if (model == null ||
                    (model.type != AqlNodeModel.Type.COLLECTION && model.type != AqlNodeModel.Type.EDGE)) {
                    collectionDetailPanel.showEmpty()
                    return
                }
                val colName = model.displayName ?: return
                collectionDetailPanel.showLoading(colName)
                ApplicationManager.getApplication().executeOnPooledThread {
                    val indexes = AqlDataService.with(project).getCollectionIndexes(colName)
                    SwingUtilities.invokeLater { collectionDetailPanel.showDetails(model, indexes) }
                }
            }
        })

        // B5: Persist expansion state across IDE restarts
        schemaTree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent)  { saveExpandedPaths() }
            override fun treeCollapsed(event: TreeExpansionEvent) { saveExpandedPaths() }
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
            restoreExpandedPaths()  // B5: bring back previously expanded nodes
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
        // C2: nodes without a model (invisible multi-server root) are treated as containers
        val isContainer = model == null || model.type.let {
            it == AqlNodeModel.Type.SERVER ||
            it == AqlNodeModel.Type.DATABASE ||
            it == AqlNodeModel.Type.CATEGORY
        }

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
        val service    = AqlDataService.with(project)
        val serverList = ServerListState.getInstance(project)

        // C2: one-time migration — keep legacy single-server users' config
        serverList.migrateIfEmpty(project.getService(DataWindowState::class.java).state)

        val servers = serverList.getServers()

        if (servers.isEmpty()) {
            schemaTree.isRootVisible = false
            schemaTree.model = DefaultTreeModel(DefaultMutableTreeNode())
            fullTreeModel = null
            setStatus(ConnectionState.NO_CONFIG)
            stopAutoRefresh()
            return
        }

        // Fetch active server with populated databases (null on connection failure)
        val activeServerData: ArangoDbServer? = if (service.hasValidSettings()) {
            try { service.server() } catch (_: Exception) { null }
        } else null

        // C2: single-server → visible root; multi-server → invisible root
        val isSingle  = servers.size == 1
        val treeModel: DefaultTreeModel
        if (isSingle) {
            schemaTree.isRootVisible = true
            treeModel = service.populateTree(activeServerData ?: servers[0], showSystemCollections)
        } else {
            schemaTree.isRootVisible = false
            treeModel = service.populateTreeMulti(servers, activeServerData, showSystemCollections)
        }

        fullTreeModel = treeModel
        applyFilter()   // respects active search text + restores expansion

        if (activeServerData?.databases?.isNotEmpty() == true) {
            lastRefreshTime = LocalTime.now()   // A8
            val state = project.getService(DataWindowState::class.java).state
            setStatus(ConnectionState.CONNECTED, "${state.host}:${state.port}")
            loadCountsAsync(treeModel)
            startAutoRefresh(state.autoRefreshMinutes)   // C1
        } else {
            stopAutoRefresh()
            setStatus(if (service.hasValidSettings()) ConnectionState.ERROR else ConnectionState.NO_CONFIG)
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
                // C2: "Set Active" switches the active server in multi-server mode
                menu.add(JMenuItem("Set Active", Icons.ICON_SELECTED).apply {
                    addActionListener { activateServer(model.tag) }
                })
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
                    addActionListener { toClipboard(model.tag ?: (model.displayName ?: "")) }
                })
            }
            AqlNodeModel.Type.DATABASE -> {
                menu.add(JMenuItem("Set Active", Icons.ICON_SELECTED).apply {
                    addActionListener { AqlDataService.with(project).setActiveDatabase(model) }
                })
                menu.addSeparator()
                menu.add(JMenuItem("Open in Console", AllIcons.Actions.Execute).apply {
                    // B7: set DB as active first so the console uses the right database
                    addActionListener {
                        AqlDataService.with(project).setActiveDatabase(model)
                        openInConsole()
                    }
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
                val parts = mutableListOf("  ● Connected")
                if (!detail.isNullOrEmpty()) parts.add(detail)
                // A8: append last-refresh timestamp
                lastRefreshTime?.let {
                    parts.add("refreshed ${it.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
                }
                statusLabel.text = parts.joinToString("  —  ")
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
     * active database (recursing into CATEGORY sub-folders added by A3) and calls
     * [DefaultTreeModel.nodeChanged] on the EDT so the renderer repaints individual
     * rows without a full tree rebuild.
     *
     * C2: uses [findActiveDbNode] to locate the selected DATABASE in any tree shape
     * (single-server visible root *or* multi-server invisible root).
     */
    private fun loadCountsAsync(treeModel: DefaultTreeModel) {
        val service = AqlDataService.with(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            val root     = treeModel.root as? DefaultMutableTreeNode ?: return@executeOnPooledThread
            val activeDb = findActiveDbNode(root)          ?: return@executeOnPooledThread
            loadCountsForNode(activeDb, service, treeModel)
        }
    }

    /**
     * Recursively locates the first DATABASE node with [AqlNodeModel.isSelected] = true.
     * Works for both single-server (SERVER as root) and multi-server (invisible root).
     */
    private fun findActiveDbNode(node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        val model = node.userObject as? AqlNodeModel
        if (model?.type == AqlNodeModel.Type.DATABASE && model.isSelected) return node
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            findActiveDbNode(child)?.let { return it }
        }
        return null
    }

    /**
     * Recursive helper for [loadCountsAsync]: loads counts for COLLECTION/EDGE nodes
     * and dives into CATEGORY folders (A3 virtual folders + System folder).
     */
    private fun loadCountsForNode(
        node: DefaultMutableTreeNode,
        service: AqlDataService,
        treeModel: DefaultTreeModel
    ) {
        for (i in 0 until node.childCount) {
            val child      = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            val childModel = child.userObject as? AqlNodeModel ?: continue
            when (childModel.type) {
                AqlNodeModel.Type.COLLECTION, AqlNodeModel.Type.EDGE -> {
                    val colName = childModel.displayName ?: continue
                    val count   = try { service.getCollectionCount(colName) } catch (_: Exception) { -1L }
                    if (count >= 0) {
                        childModel.count = count
                        SwingUtilities.invokeLater { treeModel.nodeChanged(child) }
                    }
                }
                AqlNodeModel.Type.CATEGORY -> loadCountsForNode(child, service, treeModel)
                else -> { /* SERVER/DATABASE/GRAPH/VIEW — no count to load */ }
            }
        }
    }

    // ─── C1: Auto-refresh scheduler ───────────────────────────────────────────

    /**
     * Starts (or restarts) a daemon background scheduler that calls
     * [AqlDataService.refreshSchemaSilent] every [intervalMinutes] minutes.
     * No-op when [intervalMinutes] ≤ 0.
     */
    private fun startAutoRefresh(intervalMinutes: Int) {
        stopAutoRefresh()
        if (intervalMinutes <= 0) return
        autoRefreshScheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "aql-auto-refresh").also { it.isDaemon = true }
        }
        autoRefreshScheduler?.scheduleWithFixedDelay(
            {
                if (!project.isDisposed) {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        if (!project.isDisposed) AqlDataService.with(project).refreshSchemaSilent()
                    }
                }
            },
            intervalMinutes.toLong(), intervalMinutes.toLong(), TimeUnit.MINUTES
        )
    }

    private fun stopAutoRefresh() {
        autoRefreshScheduler?.shutdownNow()
        autoRefreshScheduler = null
    }

    // ─── C2: Multi-server activation ──────────────────────────────────────────

    /**
     * Identifies the server from [tag] ("host:port"), updates [DataWindowState]
     * so it becomes the active server, and triggers a schema refresh.
     */
    private fun activateServer(tag: String?) {
        if (tag == null) return
        val colonIdx = tag.lastIndexOf(':')
        if (colonIdx < 1) return
        val host = tag.substring(0, colonIdx)
        val port = tag.substring(colonIdx + 1).toIntOrNull() ?: return
        val server = ServerListState.getInstance(project)
            .getServers().firstOrNull { it.host == host && it.port == port } ?: return
        project.getService(DataWindowState::class.java).loadState(server)
        AqlDataService.with(project).refreshSchema()
    }

    // ─── B5: Expansion-state persistence ─────────────────────────────────────

    /**
     * Builds a stable path key for [node] by joining each ancestor's label
     * (with count suffixes like " (5)" stripped) using "›" as separator.
     * E.g. "127.0.0.1›mydb›Collections" — survives schema size changes.
     */
    private fun pathKey(node: DefaultMutableTreeNode): String =
        node.path.joinToString("›") { treeNode ->
            val m   = (treeNode as? DefaultMutableTreeNode)?.userObject as? AqlNodeModel
            val raw = m?.displayName?.takeIf { it.isNotBlank() } ?: m?.name ?: treeNode.toString()
            raw.replace(Regex("\\s*\\(\\d+\\)$"), "")
        }

    /** Saves the set of currently expanded node paths to [AqlConsoleStateService]. */
    private fun saveExpandedPaths() {
        val expanded = mutableListOf<String>()
        for (row in 0 until schemaTree.rowCount) {
            if (!schemaTree.isExpanded(row)) continue
            val path = schemaTree.getPathForRow(row) ?: continue
            val node = path.lastPathComponent as? DefaultMutableTreeNode ?: continue
            expanded.add(pathKey(node))
        }
        AqlConsoleStateService.getInstance(project).state.treeExpandedPaths = expanded
    }

    /**
     * Expands all nodes whose [pathKey] matches a previously saved path.
     * Called by [applyFilter] when the filter is blank (full model shown).
     */
    private fun restoreExpandedPaths() {
        val saved = AqlConsoleStateService.getInstance(project).state.treeExpandedPaths
        if (saved.isEmpty()) return
        for (row in 0 until schemaTree.rowCount) {
            val path = schemaTree.getPathForRow(row) ?: continue
            val node = path.lastPathComponent as? DefaultMutableTreeNode ?: continue
            if (pathKey(node) in saved) schemaTree.expandRow(row)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun toClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun getContent(): JPanel = panel
    fun getProject(): Project = project

    override fun dispose() {
        stopAutoRefresh()   // C1: ensure the background thread is cleaned up
    }
}
