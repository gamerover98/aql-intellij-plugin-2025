package com.arangodb.intellij.aql.grammar.custom.psi

import com.arangodb.intellij.aql.grammar.custom.psi.impl.AqlIdMixin
import com.arangodb.intellij.aql.grammar.custom.psi.impl.AqlNamedFunctionMixin
import com.arangodb.intellij.aql.grammar.custom.psi.impl.AqlSystemPropertyMixin
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.arangodb.intellij.aql.util.Icons
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.util.IncorrectOperationException
import javax.swing.Icon

object AqlPsiUtil {

    @JvmStatic
    fun getPresentation(element: AqlNamedElement): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = element.aqlType.name
        override fun getLocationString(): String = element.containingFile.name
        override fun getIcon(unused: Boolean): Icon = when (element) {
            is AqlNamedFunctionMixin -> Icons.ICON_FUNCTION
            is AqlIdMixin -> Icons.ICON_ID
            is AqlSystemPropertyMixin -> Icons.ICON_PROPERTY
            else -> Icons.ICON_ARANGO_SMALL
        }
    }

    @JvmStatic
    fun getName(element: AqlNamedElement): String = element.text

    @JvmStatic
    fun getFunctionName(element: AqlNamedElement): String = getFunctionName(element as PsiElement)

    @JvmStatic
    fun getFunctionName(element: PsiElement): String {
        val text = element.text
        val idx = text.indexOf('(')
        return if (idx > 0) text.substring(0, idx) else text
    }

    @JvmStatic
    @Throws(IncorrectOperationException::class)
    fun setName(element: AqlNamedElement, newName: String): AqlNamedElement {
        val node = element.node
        val idNode = node.findChildByType(AqlTypes.ID)
        if (idNode is LeafElement) {
            idNode.replaceWithText(newName)
            return element
        }
        var child = node.firstChildNode
        while (child != null) {
            if (child is LeafElement && child.elementType != TokenType.WHITE_SPACE) {
                child.replaceWithText(newName)
                return element
            }
            child = child.treeNext
        }
        throw IncorrectOperationException("Rename not supported for element type: ${element.aqlType}")
    }
}
