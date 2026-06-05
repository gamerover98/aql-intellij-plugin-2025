package com.arangodb.intellij.aql.ui.windows

import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.ui.actions.CollapseAllAction
import com.arangodb.intellij.aql.ui.actions.DeleteQueryAction
import com.arangodb.intellij.aql.ui.actions.EditQueryAction
import com.arangodb.intellij.aql.ui.actions.ExecuteQueryAction
import com.arangodb.intellij.aql.ui.actions.ExpandAllAction
import com.arangodb.intellij.aql.ui.actions.ExplainQueryAction
import com.arangodb.intellij.aql.ui.panels.JsonPanel
import com.arangodb.intellij.aql.ui.renderers.AqlQueryRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultTreeModel

class AqlConsoleWindow(private val project: Project, toolWindow: ToolWindow) : Disposable {

    companion object {
        const val WINDOW_ID = "ArangoDB Console"
    }

    @JvmField var panel: JPanel? = null
    @JvmField var tabContainer: JBTabbedPane? = null
    @JvmField var jsonResults: JPanel? = null
    @JvmField var jsonTabPanel: JPanel? = null
    @JvmField var queryHistory: JPanel? = null
    @JvmField var graphPanel: JPanel? = null
    @JvmField var queryTree: Tree? = null

    private var jsonPanel: JsonPanel? = null
    private var toolbarDecorator: ToolbarDecorator? = null

    init {
        jsonResults?.setBorder(JBUI.Borders.empty())
        jsonPanel = JsonPanel(project)
        jsonResults?.add(jsonPanel, java.awt.BorderLayout.CENTER)

        project.messageBus.connect().subscribe(ActionBusEvent.AQL_QUERY_RESULT, ActionBusEvent { data -> processQuery(data) })
        project.messageBus.connect().subscribe(ActionBusEvent.AQL_SYSTEM_EMPTY_LOG, ActionBusEvent { _ -> emptyLog() })
        project.messageBus.connect().subscribe(ActionBusEvent.AQL_QUERY_TREE_CHANGE, ActionBusEvent { _ -> fillTree() })

        val consoleActionGroup = ActionManager.getInstance().getAction(ActionBusEvent.ACTION_CONSOLE) as? ActionGroup
        if (consoleActionGroup != null) {
            val consoleToolbar = ActionManager.getInstance().createActionToolbar(WINDOW_ID, consoleActionGroup, false)
            jsonTabPanel?.add(consoleToolbar.component, java.awt.BorderLayout.NORTH)
        }
        jsonTabPanel?.setBorder(CustomLineBorder(0, 0, 0, 1))
        jsonTabPanel?.validate()

        queryTree?.let { tree ->
            toolbarDecorator = ToolbarDecorator.createDecorator(tree).apply {
                setPanelBorder(BorderFactory.createEmptyBorder())
                setToolbarPosition(ActionToolbarPosition.TOP)
                addExtraAction(ExpandAllAction(tree))
                addExtraAction(CollapseAllAction(tree))
                addExtraAction(DeleteQueryAction(project, tree))
                addExtraAction(EditQueryAction(project, tree))
                addExtraAction(ExplainQueryAction(project, tree))
                addExtraAction(ExecuteQueryAction(project, tree))
            }
            queryHistory?.add(toolbarDecorator!!.createPanel())
        }

        fillTree()
    }

    private fun fillTree() {
        val tree = queryTree ?: return
        tree.cellRenderer = AqlQueryRenderer()
        tree.isRootVisible = true
        tree.showsRootHandles = false
        val treeModel: DefaultTreeModel = AqlDataService.with(project).populateQueryTree()
        tree.model = treeModel
    }

    private fun emptyLog() {
        jsonPanel?.onClean(project)
    }

    private fun processQuery(data: ActionEventData) {
        jsonPanel?.onMessage(data, project)
    }

    fun getContent(): JComponent = panel!!

    override fun dispose() {
        jsonPanel?.dispose()
    }
}
