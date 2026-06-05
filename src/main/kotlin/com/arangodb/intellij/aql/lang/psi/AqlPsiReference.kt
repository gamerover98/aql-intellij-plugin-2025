package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.fileTypes.AqlFile
import com.arangodb.intellij.aql.fileTypes.AqlFileType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.IncorrectOperationException

/**
 * Abstract base class for AQL PSI references.
 *
 * A [PsiReference] in IntelliJ Platform is a link from one PSI element to another,
 * typically used for features like navigation, refactoring, and code completion.
 * > It allows the IDE to resolve references (such as variable names, function calls, etc.)
 *   to their declarations or definitions within the codebase.
 *
 * This class provides the foundation for resolving and renaming AQL named elements
 * in the context of the IntelliJ plugin, enabling advanced IDE features for AQL files.
 *
 */
abstract class AqlPsiReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : PsiReferenceBase<PsiElement>(element, rangeInElement),
    PsiPolyVariantReference {

    /** The PSI element that this reference points to. */
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        // Check if the element is an AqlNamedElement.
        if (myElement !is AqlNamedElement) {
            return ResolveResult.EMPTY_ARRAY
        }

        // Get the PSI element type
        val elementType = PsiUtilCore.getElementType(myElement)

        // If the type is not defined, it cannot be resolved.
        if (elementType == null) {
            return ResolveResult.EMPTY_ARRAY
        }

        // Find all AqlNamedElement elements in the project
        // and convert them to an array of ResolveResult.
        return asArray<AqlNamedElement?>(
            findAll<AqlNamedElement?>(myElement.project)
        )
    }

    /** Resolves this reference to a single target element, if possible. */
    override fun resolve(): PsiElement? =
        multiResolve(false).firstOrNull()?.element

    /** Renames the element referenced by this PSI reference. */
    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String) =
        (myElement as PsiNamedElement).apply { setName(newElementName) }

    /**
     * Converts a set of [AqlNamedElement] instances into an array of [ResolveResult].
     *
     * @param data The set of named elements to convert.
     * @param <T>  The type of named element.
     * @return An array of resolve results wrapping the given elements.
     */
    protected fun <T : AqlNamedElement?> asArray(data: MutableSet<T?>): Array<ResolveResult> {
        return data
            .mapNotNull { it?.let { PsiElementResolveResult(it) } }
            .toTypedArray()
    }

    fun <T : AqlNamedElement?> findAll(project: Project): MutableSet<T?> {
        val result = findInjected<T?>(project)
        val scope = GlobalSearchScope.allScope(project)
        val virtualFiles = FileTypeIndex.getFiles(AqlFileType, scope)
        for (virtualFile in virtualFiles) {
            val file = PsiManager.getInstance(project).findFile(virtualFile) as AqlFile?
            if (file != null) {
                val found = processFile<T?>(file)
                result.addAll(found)
            }
        }
        return result
    }

    // NOTE: What did the author (M. Milicevic) mean by "Find injected references"?
    private fun <T : AqlNamedElement?> findInjected(project: Project?): MutableSet<T?> {
        return HashSet() //TODO: Find injected references.
    }

    /**
     * Processes the given [AqlFile] and collects all
     * PSI elements of type [AqlNamedElement] that
     * match the reference criteria.
     *
     * @param file The AQL file to process.
     * @param <T>  The type of PSI element to collect.
     * @return A set of matching PSI elements.
     */
    @Suppress("UNCHECKED_CAST") // 'T?' is expected to be a subtype of PsiElement.
    fun <T : PsiElement?> processFile(file: AqlFile): MutableSet<T?> {
        val nodes = HashSet<ASTNode?>()

        // Recursively populate the set with AST nodes.
        populateASTNodes(nodes, file.node)

        // Convert the collected AST nodes to PSI elements of type T.
        return nodes.mapNotNull { it?.psi as? T }.toMutableSet()
    }

    /**
     * Recursively traverses the AST starting from the given root
     * node and collects all nodes representing [AqlNamedElement]
     * instances that match the reference name and type.
     *
     * @param nodes    the set to collect matching AST nodes
     * @param rootNode the current AST node to process
     */
    fun populateASTNodes(nodes: MutableSet<ASTNode?>, rootNode: ASTNode) {
        val psi = rootNode.psi

        if (psi is AqlNamedElement) {
            val me = myElement as AqlNamedElement
            if (psi.aqlType == me.aqlType) {
                val name = psi.name

                if (name != null && name == me.getName()) {
                    nodes.add(rootNode)
                }
            }
        }

        var childNode = rootNode.firstChildNode
        while (childNode != null) {
            populateASTNodes(nodes, childNode)
            childNode = childNode.treeNext
        }
    }
}