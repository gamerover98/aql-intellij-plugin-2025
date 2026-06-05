package com.arangodb.intellij.aql.ui

import com.arangodb.intellij.aql.util.JSON
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

class TestHelpFiles {

    private val log = LoggerFactory.getLogger(TestHelpFiles::class.java)

    // @Test // TODO: fixme: uncomment and fix the test
    fun testFiles() {
        try {
            javaClass.getResourceAsStream("/testData/functions.json")?.use { stream ->
                val string = CharStreams.toString(InputStreamReader(stream, Charsets.UTF_8))
                val funObject = JSON.fromJson(string, FunObject::class.java)
                for (function in (funObject?.functions ?: emptyArray())) {
                    val name = "/docs/${function.name}.html"
                    if (javaClass.getResourceAsStream(name) == null) {
                        log.warn("Missing documentation for function: {}", name)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("", e)
        }
    }

    private data class FunObject(
        var name: String? = null,
        var functions: Array<FunObject> = emptyArray()
    )
}
