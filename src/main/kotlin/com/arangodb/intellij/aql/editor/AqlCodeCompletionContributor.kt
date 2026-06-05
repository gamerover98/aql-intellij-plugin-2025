package com.arangodb.intellij.aql.editor

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class AqlCodeCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, AqlKeywordCompletionProvider.PATTERN, AqlKeywordCompletionProvider())
        extend(CompletionType.BASIC, AqlKeywordCompletionProvider.PATTERN, AqlDatabaseCompletionProvider())
        extend(CompletionType.BASIC, AqlKeywordCompletionProvider.PATTERN, AqlContextCompletionProvider())
    }
}
