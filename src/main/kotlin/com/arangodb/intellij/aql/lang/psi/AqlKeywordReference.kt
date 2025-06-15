package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Objects

/**
 * A reference to an AQL keyword in the PSI tree.
 *
 * This class extends [AqlPsiReference] to provide functionality for
 * referencing AQL keywords, allowing for code completion and navigation
 * to the keyword definitions.
 *
 * Example: In the query `FOR doc IN collection`,
 *          the `FOR` keyword is referenced by this class.
 *
 * @param element The PSI element that this reference points to.
 * @param rangeInElement The text range within the element that this reference covers.
 */
class AqlKeywordReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : AqlPsiReference(element, rangeInElement) {

    override fun getVariants(): Array<Any> {
        return findAll<AqlNamedElement>(myElement.project)
            .filter { Objects.nonNull(it) }  // Required by LookupElementBuilder.create(...) method.
            .map {
                LookupElementBuilder
                    .create(it as AqlNamedElement)
                    .withIcon(Icons.ICON_ARANGO_SMALL)
                    .withTypeText("keyword $it()")
                    .bold()
            }
            .toTypedArray()
    }
}