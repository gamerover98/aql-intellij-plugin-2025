package com.arangodb.intellij.aql.lang

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.intellij.model.Pointer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Optional
import java.util.concurrent.TimeUnit

class AqlDocumentationTarget(
    private val key: String,
    private val docType: DocType
) : DocumentationTarget {

    enum class DocType { LANGUAGE, COLLECTION, GRAPH, VIEW }

    override fun createPointer(): Pointer<out DocumentationTarget> = Pointer { this }

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder(key).presentation()

    override fun computeDocumentation(): DocumentationResult? {
        val html = when (docType) {
            DocType.LANGUAGE -> htmlCache.get(key).orElse(null) ?: return null
            DocType.COLLECTION -> "<b>$key</b><br/>ArangoDB collection"
            DocType.GRAPH -> "<b>$key</b><br/>ArangoDB graph"
            DocType.VIEW -> "<b>$key</b><br/>ArangoDB view"
        }
        return DocumentationResult.documentation(embedImages(html))
    }

    companion object {
        private val log = Logger.getInstance(AqlDocumentationTarget::class.java)

        // Matches src attributes referencing image files (relative or absolute paths).
        private val IMAGE_SRC_REGEX =
            Regex("""src="([^"]+\.(png|jpe?g|gif|svg))"""", RegexOption.IGNORE_CASE)

        // Embeds <img src="..."> as base64 data URIs so the HTML renderer can display
        // them without needing an external URL resolver. Relative paths are resolved
        // against /docs/; absolute paths are used as-is.
        private fun embedImages(html: String): String =
            html.replace(IMAGE_SRC_REGEX) { match ->
                val src = match.groupValues[1]
                val resourcePath = if (src.startsWith("/")) src else "/docs/$src"
                val ext = resourcePath.substringAfterLast('.').lowercase()
                val mime = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "gif"         -> "image/gif"
                    "svg"         -> "image/svg+xml"
                    else          -> "image/png"
                }
                val encoded = loadBase64(resourcePath)
                if (encoded != null) """src="data:$mime;base64,$encoded"""" else match.value
            }

        private fun loadBase64(resourcePath: String): String? =
            try {
                AqlDocumentationTarget::class.java
                    .getResourceAsStream(resourcePath)
                    ?.use { Base64.getEncoder().encodeToString(it.readBytes()) }
            } catch (_: Exception) { null }

        // Guava caches don't allow null values; Optional wraps nullable HTML.
        private val htmlCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build(object : CacheLoader<String, Optional<String>>() {
                override fun load(key: String): Optional<String> {
                    return try {
                        val html = AqlDocumentationTarget::class.java
                            .getResourceAsStream("/docs/$key.html")
                            ?.use { InputStreamReader(it, StandardCharsets.UTF_8).readText() }
                        Optional.ofNullable(html)
                    } catch (e: Exception) {
                        log.error("Error loading documentation for: $key", e)
                        Optional.empty()
                    }
                }
            })
    }
}
