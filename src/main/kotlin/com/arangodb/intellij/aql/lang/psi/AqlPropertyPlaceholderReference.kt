package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Objects

/**
 * Reference implementation for AQL property placeholders in the PSI tree.
 *
 * This class is used to resolve and provide code completion for property placeholders.
 *
 * Example: TODO: add an example
 *
 * @param element The PSI element representing the placeholder.
 * @param rangeInElement The text range within the element.
 */
class AqlPropertyPlaceholderReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : AqlPsiReference(element, rangeInElement) {

    override fun getVariants(): Array<Any> {
        return findAll<AqlNamedElement>(myElement.project)
            .filter { Objects.nonNull(it) }  // Required by LookupElementBuilder.create(...) method.
            .map {
                LookupElementBuilder
                    .create(it as AqlNamedElement)
                    .withIcon(Icons.ICON_PLACEHOLDER)
                    .withTypeText("placeholder\${$it}")
                    .bold()
            }
            .toTypedArray()
    }
}