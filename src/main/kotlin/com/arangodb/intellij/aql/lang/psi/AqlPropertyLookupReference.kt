package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Objects

/**
 * Provides property lookup suggestions for AQL properties in the editor.
 *
 * This reference class is used to offer code completion variants
 * for AQL property names, leveraging the IntelliJ PSI infrastructure.
 *
 * Example usage:
 * ```
 * // When a user types a property access in an AQL query,
 * // this class supplies the list of possible property
 * // names for code completion.
 * FOR doc IN collection
 * RETURN doc.   <---- caret position
 * // At the caret position, the completion
 * // popup will show all available properties.
 * ```
 *
 * @param element The PSI element that this reference points to.
 * @param rangeInElement The text range within the element that this reference covers.
 */
class AqlPropertyLookupReference(
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