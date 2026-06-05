package com.arangodb.intellij.aql.editor

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext

class AqlDatabaseCompletionProvider : AqlCompletionProvider() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        executeService(parameters) { service ->
            result.addAllElements(service.getAll())
        }
    }
}
