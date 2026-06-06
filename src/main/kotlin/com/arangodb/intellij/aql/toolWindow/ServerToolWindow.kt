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
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Tool window for managing ArangoDB servers and databases within the IDE.
 *
 * Displays a tree structure of servers and databases with:
 *  - A1: Status bar at the bottom: ● Connected / ✕ Error / (no config)
 *  - A4: System collections grouped in a collapsible "System (N)" folder
 *  - A5: Document counts loaded asynchronously for the active database's collections
 *  - A7: Friendly empty-state message when no connection is configured
 *
 * @property project The IntelliJ project context.
 */
class ServerToolWindow(private val project: Project) : Disposable {

    companion object {
        /** Unique identifier for the ArangoDB tool window. */
        const val WINDOW_ID = "ArangoToolWindow"
    }

    // ─── Tree ─────────────────────────────────────────────────────────────────

    private val schemaTree = Tree()

    // ─── Status bar (A1) ─────────────────────────────────────────────────────

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

        val decoratorPanel = ToolbarDecorator
            .createDecorator(schemaTree)
            .apply {
                setPanelBorder(BorderFactory.createEmptyBorder())
                setToolbarPosition(com.intellij.openapi.actionSystem.ActionToolbarPosition.TOP)
                addExtraAction(AddServerAction(project))
                addExtraAction(RefreshSchemeAction(project))
                addExtraAction(ExpandAllAction(schemaTree))
                addExtraAction(CollapseAllAction(schemaTree))
                addExtraAction(SetActiveAction(project))
            }.createPanel()

        schemePanel.add(decoratorPanel, BorderLayout.CENTER)
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

        // A7: empty-state message when no server is configured
        schemaTree.emptyText.setText("No ArangoDB connection configured")
        schemaTree.emptyText.appendText(
            "  —  click  +  to add a server",
            SimpleTextAttributes.GRAYED_ATTRIBUTES
        )

        // Double-click on a collection/edge → run a sample query
        schemaTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val path = schemaTree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? CheckedTreeNode ?: return
                val model = node.userObject as? AqlNodeModel ?: return
                if (model.type != AqlNodeModel.Type.COLLECTION && model.type != AqlNodeModel.Type.EDGE) return
                val collectionName = model.displayName ?: return
                AqlDataService.with(project)
                    .executeQuery("FOR doc IN `$collectionName` LIMIT 100 RETURN doc")
                ToolWindowManager.getInstance(project)
                    .getToolWindow(AqlConsoleWindow.WINDOW_ID)
                    ?.activate(null, true)
            }
        })
    }

    // ─── Tree population ──────────────────────────────────────────────────────

    private fun fillTree() {
        val service = AqlDataService.with(project)

        // A7: no settings → show empty tree so emptyText is visible
        if (!service.hasValidSettings()) {
            schemaTree.isRootVisible = false
            schemaTree.model = DefaultTreeModel(DefaultMutableTreeNode())
            setStatus(ConnectionState.NO_CONFIG)
            return
        }

        val server = service.server()
        val treeModel = service.populateTree(server)
        schemaTree.isRootVisible = true
        schemaTree.model = treeModel

        if (server.databases.isNotEmpty()) {
            val state = project.getService(DataWindowState::class.java).state
            setStatus(ConnectionState.CONNECTED, "${state.host}:${state.port}")
            loadCountsAsync(treeModel)   // A5
        } else {
            setStatus(ConnectionState.ERROR)
        }
    }

    // ─── A1: Connection status bar ────────────────────────────────────────────

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
     * Off the EDT: for each COLLECTION/EDGE node in the active database, fetches
     * the document count via [AqlDataService.getCollectionCount] and updates the
     * node model; each update fires [DefaultTreeModel.nodeChanged] back on the EDT
     * so the renderer repaints the individual row without a full tree rebuild.
     *
     * Only the active (selected) database is queried — other databases' counts
     * remain `null` (not displayed) until the user switches the active database.
     */
    private fun loadCountsAsync(treeModel: DefaultTreeModel) {
        val service = AqlDataService.with(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            val root = treeModel.root as? DefaultMutableTreeNode ?: return@executeOnPooledThread
            for (i in 0 until root.childCount) {
                val dbNode = root.getChildAt(i) as? DefaultMutableTreeNode ?: continue
                val dbModel = dbNode.userObject as? AqlNodeModel ?: continue
                if (!dbModel.isSelected) continue   // only active database

                for (j in 0 until dbNode.childCount) {
                    val colNode = dbNode.getChildAt(j) as? DefaultMutableTreeNode ?: continue
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
                break   // only one active database
            }
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun getContent(): JPanel = panel
    fun getProject(): Project = project

    override fun dispose() { /* nothing */ }
}
