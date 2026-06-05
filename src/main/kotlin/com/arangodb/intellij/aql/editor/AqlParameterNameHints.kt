package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.grammar.generated.psi.AqlNamedFunctions
import com.arangodb.intellij.aql.intentions.AqlLanguageBundle
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement

// TODO implement
class AqlParameterNameHints : InlayParameterHintsProvider {

    companion object {
        private const val HINTS_NON_LITERALS = "aql.paramHints.nonLiterals"
        private val OPTION_HINTS_NON_LITERALS = Option(
            HINTS_NON_LITERALS,
            { AqlLanguageBundle.message(HINTS_NON_LITERALS) },
            false
        )
    }

    override fun getParameterHints(element: PsiElement): List<InlayInfo> =
        if (element is AqlNamedFunctions) emptyList() else emptyList()

    override fun getHintInfo(psiElement: PsiElement): HintInfo? = null

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getSupportedOptions(): List<Option> = listOf(OPTION_HINTS_NON_LITERALS)

    override fun isBlackListSupported(): Boolean = false
}
