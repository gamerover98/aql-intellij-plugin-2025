package com.arangodb.intellij.aql.grammar.custom

import com.arangodb.intellij.aql.lang.AqlLanguage
import com.intellij.psi.tree.IElementType

class AqlElementType(private val typeName: String) : IElementType(typeName, AqlLanguage) {
    fun getName(): String = typeName
    override fun toString(): String = typeName
}
