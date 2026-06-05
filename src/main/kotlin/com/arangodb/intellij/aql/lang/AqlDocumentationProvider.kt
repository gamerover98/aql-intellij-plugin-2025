package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.grammar.generated.psi.AqlKeywordStatements
import com.arangodb.intellij.aql.grammar.generated.psi.AqlNamedFunctions
import com.arangodb.intellij.aql.grammar.generated.psi.AqlPropertyName
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class AqlDocumentationProvider : AbstractDocumentationProvider() {

    private val log = Logger.getInstance('#' + AqlDocumentationProvider::class.java.name)

    private val documentationCache: LoadingCache<String, String> =
        CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build(object : CacheLoader<String, String>() {
                override fun load(key: String): String {
                    val notFound = "No documentation found for: '$key'"
                    return try {
                        AqlDocumentationProvider::class.java
                            .getResourceAsStream("/docs/$key.html")
                            ?.use { stream ->
                                InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                            } ?: notFound
                    } catch (_: Exception) {
                        notFound
                    }
                }
            })

    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String? {
        if (element is AqlKeywordStatements) {
            return element.node.chars.toString()
        }
        return null
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String {
        if (element is AqlPropertyName) {
            return "No documentation for '${element.text}' found."
        }
        if (element is AqlNamedElement) {
            val name: String? = when (element) {
                is AqlKeywordStatements -> element.node.chars.toString()
                is AqlNamedFunctions -> element.text
                else -> element.name
            }
            if (name != null) return loadDocumentForName(name.uppercase())
        }
        return "<no documentation>"
    }

    private fun loadDocumentForName(key: String): String =
        try {
            documentationCache.get(key)
        } catch (e: ExecutionException) {
            log.error("Error loading documentation for: $key", e)
            "No documentation found"
        }
}
