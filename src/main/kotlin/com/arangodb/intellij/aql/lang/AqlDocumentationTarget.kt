package com.arangodb.intellij.aql.lang

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.intellij.model.Pointer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import java.awt.Image
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

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

        // Pre-load images referenced in the HTML into a URL→Image map.
        // The map keys must match the src attribute values exactly.
        val imageMap: Map<String, Image> = IMAGE_URL_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .mapNotNull { url ->
                val resourcePath = if (url.startsWith("/")) url else "/docs/$url"
                loadImage(resourcePath)?.let { url to it }
            }
            .toMap()

        return if (imageMap.isEmpty()) {
            DocumentationResult.documentation(html)
        } else {
            DocumentationResult.documentation(html).images(imageMap)
        }
    }

    private fun loadImage(resourcePath: String): Image? =
        try {
            AqlDocumentationTarget::class.java
                .getResourceAsStream(resourcePath)
                ?.use { ImageIO.read(it) }
        } catch (_: Exception) { null }

    companion object {
        private val log = Logger.getInstance(AqlDocumentationTarget::class.java)
        private val IMAGE_URL_REGEX =
            Regex("""src="([^"]+\.(png|jpe?g|gif|svg))"""", RegexOption.IGNORE_CASE)

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
