package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider

abstract class AqlCompletionProvider : CompletionProvider<CompletionParameters>() {

    protected fun executeService(parameters: CompletionParameters, runnable: (AqlDatabaseService) -> Unit) {
        val project = parameters.editor.project ?: return
        val service = project.getService(AqlDatabaseService::class.java)
        runnable(service)
    }
}
