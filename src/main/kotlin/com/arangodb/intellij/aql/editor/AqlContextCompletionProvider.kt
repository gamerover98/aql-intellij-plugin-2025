package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.grammar.generated.psi.AqlPropertyName
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class AqlContextCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val containingFile = position.containingFile
        val elements = PsiTreeUtil.collectElements(containingFile) { it is AqlPropertyName }
        result.addAllElements(elements.mapNotNull { e ->
            val property = e as? AqlPropertyName ?: return@mapNotNull null
            LookupElementBuilder.create(property.text)
                .withCaseSensitivity(true)
                .withIcon(Icons.ICON_ARANGO_SMALL)
                .bold()
        })
    }
}
