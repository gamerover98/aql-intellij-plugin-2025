package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Objects

/**
 * Reference to an AQL property identifier in the PSI tree.
 *
 * This class extends [AqlPsiReference] to provide code completion and navigation
 * for AQL property identifiers (variables, fields, etc.) in the editor.
 *
 * Example usage:
 * ```
 * // Suppose foo is a property in an AQL query:
 * // The reference will resolve 'foo' for completion and navigation.
 * FOR doc IN collection
 * RETURN doc.foo
 * ```
 *
 * @param element The PSI element that this reference points to.
 * @param rangeInElement The text range within the element that this reference covers.
 */
class AqlPropertyIdReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : AqlPsiReference(element, rangeInElement) {

    override fun getVariants(): Array<Any> {
        return findAll<AqlNamedElement>(myElement.project)
            .filter { Objects.nonNull(it) }  // Required by LookupElementBuilder.create(...) method.
            .map {
                LookupElementBuilder
                    .create(it as AqlNamedElement)
                    .withIcon(Icons.ICON_ID)
                    .withTypeText("variable $it()")
                    .bold()
            }
            .toTypedArray()
    }
}