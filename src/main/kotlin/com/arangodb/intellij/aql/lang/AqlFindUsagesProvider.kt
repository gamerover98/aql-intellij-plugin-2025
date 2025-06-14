package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.grammar.custom.psi.AqlLexerAdapter
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

class AqlFindUsagesProvider : FindUsagesProvider {

    /** Returns the name of the language for which this provider is applicable. */
    override fun getWordsScanner(): WordsScanner? =
        DefaultWordsScanner(
            AqlLexerAdapter(),
            // Identifier tokens for AQL
            TokenSet.create(
                AqlTypes.NAMED_FUNCTIONS,
                AqlTypes.PROPERTY_NAME,
                AqlTypes.NAMED_KEYWORD_STATEMENTS,
                AqlTypes.NAMED_KEYWORD_STATEMENTS
            ),
            STRING_LITERALS, // String literals in AQL
            // Other literal tokens in AQL
            TokenSet.create(
                AqlTypes.NUMBER_INTEGER,
                AqlTypes.STRING_TYPE,
                AqlTypes.T_NULL,
                AqlTypes.T_TRUE,
                AqlTypes.T_FALSE
            ),
            // Skip
            TokenSet.create(AqlTypes.OPERATOR_STATEMENTS)
        )

    /** True if the element is an [AqlNamedElement], meaning usages can be found. */
    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement is AqlNamedElement

    /**
     * Returns the help ID for the given PSI element.
     *
     * This method uses the internal constant [HelpID.FIND_OTHER_USAGES]
     * because there is no public or specific help ID available for AQL usages.
     * Using this internal value ensures that the IDE provides a generic help
     * link for "Find Usages" actions, even though it is not officially
     * documented for external use.
     *
     * @param psiElement The PSI element for which help is requested.
     * @return The help ID string, or null if not available.
     */
    override fun getHelpId(psiElement: PsiElement): String? {
        return HelpID.FIND_OTHER_USAGES
    }

    /**
     * Returns the type of the given PSI element as a string.
     *
     * @param element The PSI element to analyze.
     * @return The type name if available, otherwise an empty string.
     */
    override fun getType(element: PsiElement): String = extractName(element) ?: ""

    /**
     * Returns a descriptive name for the given PSI element.
     *
     * @param element the PSI element to describe
     * @return the extracted name, or an empty string if not available
     */
    override fun getDescriptiveName(element: PsiElement): String = extractName(element) ?: ""

    /** Returns the text of the given PSI element, using its descriptive name. */
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return getDescriptiveName(element)
    }

    /**
     * Extracts the name from the given PSI element if it is an [AqlNamedElement].
     *
     * @param element The PSI element to extract the name from.
     * @return The name if available, an empty string if the name
     *         is null, or null if the element is not an AqlNamedElement.
     */
    private fun extractName(element: PsiElement): String? {
        return when (element) {
            is AqlNamedElement -> element.name ?: ""
            else -> null
        }
    }
}