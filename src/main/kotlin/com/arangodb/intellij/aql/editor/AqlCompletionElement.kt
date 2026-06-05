package com.arangodb.intellij.aql.editor

import com.intellij.codeInsight.lookup.LookupElement

fun interface AqlCompletionElement {
    fun createLookupElement(): LookupElement
}
