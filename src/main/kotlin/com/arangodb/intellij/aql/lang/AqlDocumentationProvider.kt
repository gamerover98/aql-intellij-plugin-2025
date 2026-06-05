package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
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
                                val html = InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                                processHtml(html)
                            } ?: notFound
                    } catch (_: Exception) {
                        notFound
                    }
                }
            })

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val aqlElement = resolveAqlElement(element ?: originalElement ?: return null) ?: return null
        return when (aqlElement) {
            is AqlKeywordStatements -> aqlElement.node.chars.toString()
            else -> aqlElement.name
        }
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = element ?: originalElement ?: return null
        val aqlElement = resolveAqlElement(target) ?: return null

        return when {
            aqlElement is AqlPropertyName -> null
            aqlElement is AqlKeywordStatements ->
                loadDocumentForName(aqlElement.node.chars.toString().uppercase())
            aqlElement is AqlNamedFunctions ->
                loadDocumentForName(aqlElement.text.uppercase())
            aqlElement.aqlType == AqlMixinType.ID ->
                docForDatabaseObject(aqlElement)
            else ->
                aqlElement.name?.let { loadDocumentForName(it.uppercase()) }
        }
    }

    // Resolves a PSI element or its parent to an AqlNamedElement.
    // IntelliJ may pass a raw leaf token; walk up one level to find the named wrapper.
    private fun resolveAqlElement(element: PsiElement): AqlNamedElement? = when {
        element is AqlNamedElement -> element
        element.parent is AqlNamedElement -> element.parent as AqlNamedElement
        else -> null
    }

    // Checks whether the identifier matches a cached collection/graph/view and returns a short HTML summary.
    // Returns null if the DB cache is empty or the name is not a known DB object.
    private fun docForDatabaseObject(element: AqlNamedElement): String? {
        val name = element.text
        val service = element.project.getService(AqlDatabaseService::class.java)
        return when {
            service.getCollections().any { it.lookupString == name } ->
                "<b>$name</b><br/>ArangoDB collection"
            service.getGraphs().any { it.lookupString == name } ->
                "<b>$name</b><br/>ArangoDB graph"
            service.getSearchViews().any { it.lookupString == name } ->
                "<b>$name</b><br/>ArangoDB view"
            else -> null
        }
    }

    // Rewrites <img src="..."> references to inline base64 data URIs so IntelliJ's
    // HTML renderer (which has no classpath URL resolver) can display them.
    private fun processHtml(html: String): String =
        html.replace(Regex("""src="([^"]+\.(png|jpe?g|gif|svg))"""", RegexOption.IGNORE_CASE)) { match ->
            val src = match.groupValues[1]
            val resourcePath = if (src.startsWith("/")) src else "/docs/$src"
            val ext = resourcePath.substringAfterLast('.').lowercase()
            val mime = when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "gif"         -> "image/gif"
                "svg"         -> "image/svg+xml"
                else          -> "image/png"
            }
            val encoded = loadImageAsBase64(resourcePath)
            if (encoded != null) """src="data:$mime;base64,$encoded"""" else match.value
        }

    private fun loadImageAsBase64(resourcePath: String): String? =
        try {
            AqlDocumentationProvider::class.java
                .getResourceAsStream(resourcePath)
                ?.use { java.util.Base64.getEncoder().encodeToString(it.readBytes()) }
        } catch (_: Exception) { null }

    private fun loadDocumentForName(key: String): String =
        try {
            documentationCache.get(key)
        } catch (e: ExecutionException) {
            log.error("Error loading documentation for: $key", e)
            "No documentation found"
        }
}
