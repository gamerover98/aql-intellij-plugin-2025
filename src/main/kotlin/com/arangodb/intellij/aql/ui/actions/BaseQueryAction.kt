package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.model.AqlQuery
import com.arangodb.intellij.aql.ui.renderers.AqlQueryModel
import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.treeStructure.Tree
import javax.swing.Icon

abstract class BaseQueryAction(name: String, description: String, icon: Icon) :
    AnActionButton(name, description, icon) {

    protected fun getSelectedQuery(project: Project, tree: Tree): AqlQuery? {
        val selectionModel = tree.selectionModel ?: return null
        val selectionPath = selectionModel.selectionPath ?: return null
        val component = selectionPath.lastPathComponent as? CheckedTreeNode ?: return null
        val userObject = component.userObject as? AqlQueryModel ?: return null
        return AqlDataService.with(project).getExistingQueryForName(userObject.name ?: "")
    }
}
