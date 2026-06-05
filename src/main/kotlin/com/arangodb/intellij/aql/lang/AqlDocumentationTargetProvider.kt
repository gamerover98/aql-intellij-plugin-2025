package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.grammar.generated.psi.AqlKeywordStatements
import com.arangodb.intellij.aql.grammar.generated.psi.AqlNamedFunctions
import com.arangodb.intellij.aql.grammar.generated.psi.AqlPropertyName
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement

// Called by the platform in a read action — PSI access is safe here.
class AqlDocumentationTargetProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        val aqlElement = when {
            element is AqlNamedElement -> element
            element.parent is AqlNamedElement -> element.parent as AqlNamedElement
            else -> return null
        }
        if (aqlElement is AqlPropertyName) return null

        val (key, type) = when {
            aqlElement is AqlKeywordStatements ->
                aqlElement.node.chars.toString().uppercase() to AqlDocumentationTarget.DocType.LANGUAGE
            aqlElement is AqlNamedFunctions ->
                aqlElement.text.uppercase() to AqlDocumentationTarget.DocType.LANGUAGE
            aqlElement.aqlType == AqlMixinType.ID -> {
                val name = aqlElement.text
                val service = aqlElement.project.getService(AqlDatabaseService::class.java)
                when {
                    service.getCollections().any { it.lookupString == name } ->
                        name to AqlDocumentationTarget.DocType.COLLECTION
                    service.getGraphs().any { it.lookupString == name } ->
                        name to AqlDocumentationTarget.DocType.GRAPH
                    service.getSearchViews().any { it.lookupString == name } ->
                        name to AqlDocumentationTarget.DocType.VIEW
                    else -> return null
                }
            }
            else ->
                aqlElement.name?.uppercase()?.let { it to AqlDocumentationTarget.DocType.LANGUAGE } ?: return null
        }

        return AqlDocumentationTarget(key, type)
    }
}
