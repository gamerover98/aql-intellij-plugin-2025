package com.arangodb.intellij.aql.lang.psi

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Objects

/**
 * Reference implementation for system properties in AQL.
 *
 * Used to provide code completion and navigation for
 * system property names within AQL queries.
 *
 * Example:
 * ```
 * FOR doc IN myCollection
 * RETURN doc._key <---- system property reference
 * // "_key" is a system property and will be resolved by this reference.
 * ```
 *
 * @param element The PSI element this reference is attached to.
 * @param rangeInElement The text range within the element that this reference covers.
 */
class AqlSystemPropertyReference(
    element: PsiElement,
    rangeInElement: TextRange?
) : AqlPsiReference(element, rangeInElement) {

    override fun getVariants(): Array<Any> {
        return findAll<AqlNamedElement>(myElement.project)
            .filter { Objects.nonNull(it) }  // Required by LookupElementBuilder.create(...) method.
            .map {
                LookupElementBuilder
                    .create(it as AqlNamedElement)
                    .withIcon(Icons.ICON_SYSTEM_ATTRIBUTE)
                    .withTypeText("system $it")
                    .bold()
            }
            .toTypedArray()
    }
}