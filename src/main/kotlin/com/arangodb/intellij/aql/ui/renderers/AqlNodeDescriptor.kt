package com.arangodb.intellij.aql.ui.renderers

import com.arangodb.intellij.aql.util.Icons
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.CellAppearanceEx
import com.intellij.openapi.roots.ui.util.CompositeAppearance
import com.intellij.openapi.util.Comparing
import com.intellij.ui.SimpleColoredComponent
import com.intellij.util.ui.UIUtil
import java.awt.Font

open class AqlNodeDescriptor : NodeDescriptor<AqlNodeModel>, CellAppearanceEx {

    private val model: AqlNodeModel
    private var myHighlightedText: CompositeAppearance

    constructor(project: Project, parentDescriptor: NodeDescriptor<*>?, target: AqlNodeModel) : super(project, parentDescriptor) {
        model = target
        myHighlightedText = CompositeAppearance()
        myHighlightedText.icon = Icons.ICON_DATABASE
        myHighlightedText.customize(AqlNodeRenderer())
    }

    constructor(project: Project, parentDescriptor: NodeDescriptor<*>?) : super(project, parentDescriptor) {
        model = AqlNodeModel("root", "root", AqlNodeModel.Type.DATABASE)
        myHighlightedText = CompositeAppearance()
    }

    override fun getElement(): AqlNodeModel = model

    fun getTarget(): AqlNodeModel = model

    override fun update(): Boolean {
        val oldText = myHighlightedText
        val isServer = model.type == AqlNodeModel.Type.SERVER
        icon = if (isServer) Icons.ICON_DATABASE else Icons.ICON_COLLECTION
        myHighlightedText = CompositeAppearance()
        val color = if (isServer) UIUtil.getLabelForeground() else UIUtil.getLabelDisabledForeground()
        val nameAttributes = TextAttributes(color, null, null, EffectType.BOXED, if (isServer) Font.BOLD else Font.PLAIN)
        myHighlightedText.ending.addText(model.displayName ?: "", nameAttributes)
        myName = myHighlightedText.text
        return !Comparing.equal(myHighlightedText, oldText)
    }

    fun getHighlightedText(): CellAppearanceEx = myHighlightedText

    override fun customize(component: SimpleColoredComponent) {
        getHighlightedText().customize(component)
        component.icon = Icons.ICON_DATABASE
        component.toolTipText = getTarget().displayName
    }

    override fun getText(): String = model.displayName ?: ""
}
