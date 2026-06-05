package com.arangodb.intellij.aql.grammar.custom.psi

import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class AqlParserUtil : GeneratedParserUtilBase() {
    companion object {
        @JvmStatic
        fun parsePercentileRange(builder: PsiBuilder, tokens: Int): Boolean {
            if (builder.tokenType == AqlTypes.NUMBER_INTEGER) {
                val value = builder.tokenText ?: return false
                val i = value.toInt()
                return i in 1..100
            }
            return false
        }

        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        fun consumeTokenFast(builder: PsiBuilder, tokens: TokenSet): Boolean {
            if (nextTokenIsFast(builder, tokens)) {
                builder.advanceLexer()
                return true
            }
            return false
        }

        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        fun nextTokenIsFast(builder: PsiBuilder, token: IElementType): Boolean =
            builder.tokenType == token

        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        fun nextTokenIsFast(builder: PsiBuilder, vararg tokens: IElementType): Boolean {
            val tokenType = builder.tokenType
            return tokens.any { it == tokenType }
        }

        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        fun nextTokenIsFast(builder: PsiBuilder, tokens: TokenSet): Boolean =
            tokens.contains(builder.tokenType)
    }
}
