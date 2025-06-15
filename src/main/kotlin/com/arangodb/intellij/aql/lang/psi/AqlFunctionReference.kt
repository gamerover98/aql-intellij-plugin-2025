package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.*

/**
 * A reference to an AQL function in the PSI tree.
 *
 * This class extends [AqlPsiReference] to provide functionality for
 * referencing AQL functions, allowing for code completion and navigation
 * to the function definitions.
 *
 * Example usage:
 * Given the AQL code: `RETURN MY_FUNCTION()`
 * The reference will resolve `MY_FUNCTION` to its definition.
 *
 * @param element The PSI element that this reference points to.
 * @param rangeInElement The text range within the element that this reference covers.
 */
class AqlFunctionReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : AqlPsiReference(element, rangeInElement) {

    override fun getVariants(): Array<Any> {
        return findAll<AqlNamedElement>(myElement.project)
            .filter { Objects.nonNull(it) }  // Required by LookupElementBuilder.create(...) method.
            .map {
                LookupElementBuilder
                    .create(it as AqlNamedElement)
                    .withIcon(Icons.ICON_FUNCTION)
                    .withTypeText("function $it()")
                    .bold()
            }
            .toTypedArray()
    }
}