package com.arangodb.intellij.aql.spring

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.util.ProcessingContext

/**
 * Provides ArangoDB collection name completions inside Spring Data `@Document` annotations.
 *
 * When the user types inside `@Document("...")`, this contributor offers the names of all
 * known collections in the active database (same set used by `AqlDatabaseCompletionProvider`
 * for `.aql` files).
 *
 * Only fires when connected — the collection list is empty when not connected.
 */
class SpringDataCompletionContributor : CompletionContributor() {

    companion object {
        private const val DOCUMENT_ANNOTATION = "com.arangodb.springframework.annotation.Document"
    }

    init {
        extend(
            CompletionType.BASIC,
            PsiJavaPatterns.psiElement()
                .inside(
                    PsiJavaPatterns.psiAnnotation().qName(DOCUMENT_ANNOTATION)
                ),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val service = parameters.position.project
                        .getService(AqlDatabaseService::class.java)
                    result.addAllElements(service.getCollections())
                }
            }
        )
    }
}
