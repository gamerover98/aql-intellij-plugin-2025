package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.grammar.custom.psi.AqlPsiUtil
import com.arangodb.intellij.aql.grammar.generated.psi.AqlFunctionExpression
import com.arangodb.intellij.aql.intentions.AqlLanguageBundle
import com.arangodb.intellij.aql.util.JSON
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AqlParameterNameHints : InlayParameterHintsProvider {

    companion object {
        private const val HINTS_NON_LITERALS = "aql.paramHints.nonLiterals"
        private val OPTION_HINTS_NON_LITERALS = Option(
            HINTS_NON_LITERALS,
            { AqlLanguageBundle.message(HINTS_NON_LITERALS) },
            false
        )

        @Volatile private var cachedParams: AqlParams? = null

        private fun getParameters(functionName: String?): List<String> {
            if (functionName == null) return emptyList()
            if (cachedParams == null) {
                try {
                    AqlParameterNameHints::class.java.getResourceAsStream("/AqlFunctionParameters.json")
                        ?.use { stream ->
                            val text = InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                            cachedParams = JSON.fromJson(text, AqlParams::class.java)
                        }
                } catch (_: IOException) {
                }
            }
            return cachedParams?.forName(functionName) ?: emptyList()
        }

        private class AqlParams {
            var params: Map<String, List<String>> = HashMap()
            fun forName(name: String): List<String> = params[name] ?: emptyList()
        }
    }

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        if (element !is AqlFunctionExpression) return emptyList()
        val functionName = AqlPsiUtil.getFunctionName(element.namedFunctions)
        val paramNames = getParameters(functionName)
        if (paramNames.isEmpty()) return emptyList()
        val args = element.expressionTypeList
        return paramNames.zip(args).map { (name, arg) ->
            InlayInfo("$name:", arg.textRange.startOffset)
        }
    }

    override fun getHintInfo(psiElement: PsiElement): HintInfo? = null

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getSupportedOptions(): List<Option> = listOf(OPTION_HINTS_NON_LITERALS)

    override fun isBlackListSupported(): Boolean = false
}
