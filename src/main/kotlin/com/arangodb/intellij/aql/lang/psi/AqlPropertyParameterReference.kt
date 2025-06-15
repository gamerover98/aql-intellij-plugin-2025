package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Objects

/**
 * Reference implementation for AQL property parameters in the PSI tree.
 *
 * Used to provide code completion and navigation for property parameters in AQL queries.
 *
 * Example:
 * ```
 * // Given the AQL query:
 * FOR doc IN collection
 * FILTER doc.value == @propertyName <---
 * RETURN doc
 * ```
 *
 * The `@propertyName` is resolved by this reference to available property parameters.
 *
 * @param element The PSI element that this reference points to.
 * @param rangeInElement The text range within the element that this reference covers.
 */
class AqlPropertyParameterReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : AqlPsiReference(element, rangeInElement) {

    override fun getVariants(): Array<Any> {
        return findAll<AqlNamedElement>(myElement.project)
            .filter { Objects.nonNull(it) }  // Required by LookupElementBuilder.create(...) method.
            .map {
                LookupElementBuilder
                    .create(it as AqlNamedElement)
                    .withIcon(Icons.ICON_PROPERTY)
                    .withTypeText("property $it()")
                    .bold()
            }
            .toTypedArray()
    }
}