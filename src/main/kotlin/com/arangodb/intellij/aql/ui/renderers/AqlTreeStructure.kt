package com.arangodb.intellij.aql.ui.renderers

import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.ArrayUtil

class AqlTreeStructure(private val myProject: Project) : AbstractTreeStructure() {

    private val myRoot = Any()

    override fun isToBuildChildrenInBackground(element: Any): Boolean = true

    override fun isAlwaysLeaf(element: Any): Boolean =
        element !== myRoot && element !is AqlNodeModel

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): AqlNodeDescriptor {
        if (element === myRoot) return RootNodeDescriptor(myProject, parentDescriptor)
        return AqlNodeDescriptor(myProject, element as? AqlNodeDescriptor)
    }

    override fun getChildElements(element: Any): Array<Any> = ArrayUtil.EMPTY_OBJECT_ARRAY

    override fun getParentElement(element: Any): Any? =
        if (element is AqlNodeModel) myRoot else null

    override fun commit() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()
    }

    override fun hasSomethingToCommit(): Boolean =
        PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments()

    override fun asyncCommit(): ActionCallback = asyncCommitDocuments(myProject)

    override fun getRootElement(): Any = myRoot

    private inner class RootNodeDescriptor(project: Project, parentDescriptor: NodeDescriptor<*>?) :
        AqlNodeDescriptor(project, parentDescriptor) {

        override fun update(): Boolean {
            myName = ""
            return false
        }
    }
}
