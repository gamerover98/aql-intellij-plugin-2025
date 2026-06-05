package com.arangodb.intellij.aql.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStreamReader

// Development utility for generating grammar BNF tokens from the built-in function JSON list.
// Run as a standalone JVM main to regenerate grammar token definitions.
object GrammarUtil {
    @JvmStatic
    fun main(args: Array<String>) {
        val stream = GrammarUtil::class.java.getResourceAsStream("/lang/builtin.json") ?: return
        val mapper = ObjectMapper()
        val holder = InputStreamReader(stream).use { mapper.readValue(it, Holder::class.java) }
        val all = holder.functions
            .map { it.name.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("_") }
            .map { printFunctionToken(it) }
            .toHashSet()
        printBnf(all)
    }

    private fun printBnf(all: Set<String>) {
        val start = "KeywordFunctions ::=  " + all.joinToString("\n                        | ")
        println(start)
    }

    private fun printFunctionToken(name: String): String {
        val lowerCase = name.lowercase()
        val tokenName = "F_$name"
        val rex = buildString {
            append("$tokenName = \"regexp:")
            name.indices.forEach { i ->
                val lowerC = lowerCase[i]
                val upperC = name[i]
                val chars = if (lowerC == upperC) "$upperC" else "$upperC$lowerC"
                append("([$chars])")
            }
            append('"')
        }
        println(rex)
        return tokenName
    }

    private class Holder {
        var functions: List<BuiltInAqlFunction> = emptyList()
    }

    private class BuiltInAqlFunction {
        var name: String = ""
        var arguments: String? = null
        var deterministic: Boolean = false
        var cacheable: Boolean = false
        var canRunOnDBServer: Boolean = false
        @JsonIgnore
        var implementations: Any? = null
    }
}
