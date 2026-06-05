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

    override fun getWordsScanner(): WordsScanner? =
        DefaultWordsScanner(
            AqlLexerAdapter(),
            // Identifier tokens for AQL
            TokenSet.create(
                AqlTypes.NAMED_FUNCTIONS,
                AqlTypes.PROPERTY_NAME,
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

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement is AqlNamedElement

    // HelpID.FIND_OTHER_USAGES is @Internal but there is no public AQL-specific help ID
    override fun getHelpId(psiElement: PsiElement): String? = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String = extractName(element) ?: ""

    override fun getDescriptiveName(element: PsiElement): String = extractName(element) ?: ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = getDescriptiveName(element)

    private fun extractName(element: PsiElement): String? = when (element) {
        is AqlNamedElement -> element.name ?: ""
        else -> null
    }
}