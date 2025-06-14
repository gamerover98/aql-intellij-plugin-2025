package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.fileTypes.AqlFile
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.arangodb.intellij.aql.util.Icons
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

/**
 * Represents a structure view element for AQL files.
 *
 * This class adapts a PSI element to the structure view, providing navigation,
 * sorting, presentation, and child element retrieval for the AQL language.
 *
 * For example, he Structure View shows an outline of the elements (such as
 * functions, variables, or statements) in an AQL file, allowing quick navigation
 * and a hierarchical overview of the file's contents.
 *
 * @property element The PSI element represented in the structure view.
 */
class AqlStructureViewElement(
    //private val log = LoggerFactory.getLogger(AqlStructureViewElement.class)
    private val element: NavigatablePsiElement
) : StructureViewTreeElement, SortableTreeElement {

    /** Returns the underlying PSI element. */
    override fun getValue() = element

    /** Navigates to the PSI element in the editor. */
    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)

    /** Returns true if the element can be navigated to. */
    override fun canNavigate() = element.canNavigate()

    /** Returns true if the element can be navigated to its source. */
    override fun canNavigateToSource() = element.canNavigateToSource()

    /** Returns the key used for alphabetical sorting in the structure view. */
    override fun getAlphaSortKey() = element.name ?: ""

    /**
     * Returns the presentation of the element for the structure view.
     *
     * If the element has a custom presentation, it is used; otherwise,
     * a default presentation is provided.
     */
    override fun getPresentation(): ItemPresentation {
        val presentation = element.presentation

        if (presentation != null) {
            return presentation
        }

        // If the element does not have a custom presentation, we create a default one.
        return object : ItemPresentation {

            /** Returns the presentable text for the structure view. */
            override fun getPresentableText() = element.text

            /** Returns the location string (file name) for the structure view. */
            override fun getLocationString() = element.containingFile.name

            /** Returns the icon for the structure view element. */
            override fun getIcon(unused: Boolean): Icon {
                // The following code was originally present in the
                // Java version of this file as commented-out code.
                // It has been retained here for reference and potential future
                // refactoring, but is currently not used in the implementation.

                //var child = element.node.firstChildNode
                //while (child != null) {
                //    if (child.elementType == AqlTypes.NAMED_FUNCTIONS) {
                //        return Icons.ICON_FUNCTION
                //    }
                //    child = child.treeNext
                //}

                return Icons.ICON_ARANGO_SMALL
            }
        }
    }

    /**
     * Returns the child elements of this structure view element as an array of [TreeElement].
     *
     * If the underlying element is not an [AqlFile], an empty array is returned.
     * Otherwise, it collects all child [ASTWrapperPsiElement]s, filters out elements
     * that are not relevant for the structure view (such as comments, operators, numbers, etc.),
     * and wraps the remaining elements in [AqlStructureViewElement] instances.
     *
     * @return An array of [TreeElement] representing the children of this element in the structure view.
     */
    override fun getChildren(): Array<out TreeElement> {
        if (element !is AqlFile) {
            // If the element is not an AqlFile, we return an empty array.
            // This is to ensure that only AQL files are processed in the structure view.
            return StructureViewTreeElement.EMPTY_ARRAY
        }

        // Get all direct children of type ASTWrapperPsiElement from
        // the current element. These represent PSI elements that wrap
        // AST nodes and are potential candidates for the structure view.
        val elements =
            PsiTreeUtil.getChildrenOfType(
                element,
                ASTWrapperPsiElement::class.java
            )

        if (elements == null) {
            // If there are no child elements, we return an empty array.
            return StructureViewTreeElement.EMPTY_ARRAY
        }

        val treeElements = ArrayList<TreeElement>(elements.size)

        // Iterate over each child element.
        for (child in elements) {
            // Get the first child node of the current element.
            val firstChildNode = child.node.firstChildNode
            if (firstChildNode != null) {
                // Get the type of the first child's first child node.
                val typeNode = firstChildNode.firstChildNode

                if (typeNode != null) {
                    val elementType = typeNode.elementType
                    // Skip elements that are not relevant for the structure view.
                    if (elementType == AqlTypes.COMMENT
                        || elementType == AqlTypes.OPERATOR_STATEMENTS
                        || elementType == AqlTypes.NUMBER
                        || elementType == AqlTypes.T_COMMA
                        || elementType == AqlTypes.T_OPEN
                        || elementType == AqlTypes.T_CLOSE
                        || elementType == AqlTypes.T_ARRAY_OPEN
                        || elementType == AqlTypes.T_ARRAY_CLOSE
                        || elementType == AqlTypes.JSON_TYPE
                        || elementType == AqlTypes.SEQUENCE
                    ) {
                        continue
                    }
                }
            }

            // Add the element to the tree if it passed the filter.
            treeElements.add(AqlStructureViewElement(child))
        }

        return treeElements.toTypedArray<TreeElement>()
    }
}