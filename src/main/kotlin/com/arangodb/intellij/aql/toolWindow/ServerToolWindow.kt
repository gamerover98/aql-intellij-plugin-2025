package com.arangodb.intellij.aql.toolWindow

import com.arangodb.intellij.aql.ArangoBundle
import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.services.ArangoProjectService
import com.arangodb.intellij.aql.ui.actions.*
import com.arangodb.intellij.aql.ui.renderers.AqlNodeModel
import com.arangodb.intellij.aql.ui.renderers.AqlNodeRenderer
import com.arangodb.intellij.aql.ui.windows.AqlConsoleWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.BorderFactory
import javax.swing.SwingUtilities

/**
 * Tool window for managing ArangoDB servers and databases within the IDE.
 *
 * Displays a tree structure of servers and databases, provides toolbar actions
 * for adding, refreshing, expanding, collapsing, and setting the active database.
 * Listens to relevant events to update the UI accordingly.
 *
 * @property project The IntelliJ project context.
 */
class ServerToolWindow(private val project: Project) : Disposable {
    companion object {
        /** Unique identifier for the ArangoDB tool window. */
        const val WINDOW_ID = "ArangoToolWindow" // Same as in plugin.xml
    }

    /** The tree component displaying the server and database schema. */
    private val schemaTree = Tree()

    /** The panel containing the schema tree and its toolbar. */
    private val schemePanel = JPanel(BorderLayout())

    /** The main content panel for the tool window. */
    private val panel: JPanel

    init {
        /** Populates the schema tree with the current server and database structure. */
        fun fillTree() {
            schemaTree.cellRenderer = AqlNodeRenderer() // Set the custom renderer for the tree nodes.
            schemaTree.isRootVisible = true
            schemaTree.showsRootHandles = true

            val service = AqlDataService.with(project)
            schemaTree.model = service.populateTree(service.server())
        }

        // Configure the schema tree and its toolbar actions.
        schemaTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val path = schemaTree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? CheckedTreeNode ?: return
                val model = node.userObject as? AqlNodeModel ?: return
                if (model.type != AqlNodeModel.Type.COLLECTION && model.type != AqlNodeModel.Type.EDGE) return
                val collectionName = model.displayName ?: return
                val service = AqlDataService.with(project)
                service.executeQuery("FOR doc IN `$collectionName` LIMIT 100 RETURN doc")
                ToolWindowManager.getInstance(project)
                    .getToolWindow(AqlConsoleWindow.WINDOW_ID)
                    ?.activate(null, true)
            }
        })

        schemePanel.add(
            ToolbarDecorator
                .createDecorator(schemaTree)
                .apply {
                    setPanelBorder(BorderFactory.createEmptyBorder())
                    setToolbarPosition(com.intellij.openapi.actionSystem.ActionToolbarPosition.TOP)
                    addExtraAction(AddServerAction(project))
                    addExtraAction(RefreshSchemeAction(project))
                    addExtraAction(ExpandAllAction(schemaTree))
                    addExtraAction(CollapseAllAction(schemaTree))
                    addExtraAction(SetActiveAction(project))
                }.createPanel(),
            BorderLayout.CENTER
        ).apply {
            fillTree() // Initial population of the tree.
        }

        val service = project.service<ArangoProjectService>()

        // Subscribe to events.
        service
            .subscribe(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME) { SwingUtilities.invokeLater { fillTree() } }
            .subscribe(ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET) {
                val selectedNodes =
                    schemaTree
                        .getSelectedNodes(CheckedTreeNode::class.java) { node ->
                            val userObject = node.userObject as? AqlNodeModel
                            userObject?.type == AqlNodeModel.Type.DATABASE
                        }
                when {
                    selectedNodes.isEmpty() -> ArangoBundle.notifyWarning("selectNodeError")
                    selectedNodes.size > 1 -> ArangoBundle.notifyWarning("selectOnlyOneNodeError")
                    else -> {
                        val selectedNode = selectedNodes.first().userObject as AqlNodeModel
                        service.setActiveDatabase(selectedNode)
                    }
                }
            }

        panel = panel {
            row {
                cell(schemePanel)
                    .align(Align.FILL)
                    .resizableColumn()
            }
        }
    }

    /**
     * @return The main content panel for the server tool window.
     */
    fun getContent(): JPanel = panel

    /**
     * @return The associated IntelliJ project.
     */
    fun getProject(): Project = project

    override fun dispose() {
        // Nothing to do.
    }
}