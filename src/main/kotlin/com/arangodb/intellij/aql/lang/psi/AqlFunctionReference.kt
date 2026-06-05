package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
/**
 * PSI reference to an AQL function — enables code completion and go-to-definition for function names.
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
            .filterIsInstance<AqlNamedElement>()
            .map {
                LookupElementBuilder
                    .create(it)
                    .withIcon(Icons.ICON_FUNCTION)
                    .withTypeText("function $it()")
                    .bold()
            }
            .toTypedArray()
    }
}