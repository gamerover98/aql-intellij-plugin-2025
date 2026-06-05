package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.arangodb.intellij.aql.lang.AqlLanguage
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

class AqlKeywordCompletionProvider : AqlCompletionProvider() {

    companion object {
        val PATTERN: ElementPattern<PsiElement> = PlatformPatterns
            .psiElement()
            .withLanguage(AqlLanguage)
            .andNot(PlatformPatterns.psiElement(AqlTypes.COMMENT))
            .andNot(PlatformPatterns.psiElement(AqlTypes.LINE_COMMENT))
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        result.addAllElements(AqlKeywords.ALL)
    }
}
