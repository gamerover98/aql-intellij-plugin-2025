package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import javax.swing.Icon

class AqlKeywordElement(
    private val keyword: String,
    private val icon: Icon = Icons.ICON_ARANGO_SMALL
) : AqlCompletionElement {

    override fun createLookupElement(): LookupElement =
        LookupElementBuilder.create(keyword)
            .withCaseSensitivity(false)
            .withIcon(icon)
            .bold()
}
