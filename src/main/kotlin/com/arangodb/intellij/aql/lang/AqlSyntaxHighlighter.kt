package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.grammar.custom.psi.AqlLexerAdapter
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Syntax highlighter for AQL (ArangoDB Query Language) files.
 *
 * This class provides token highlighting for AQL code in the IntelliJ editor,
 * mapping token types to specific color attributes for improved readability.
 *
 * > It uses a custom lexer and defines highlight rules for comments, strings,
 * > functions, keywords, variables, numbers, and other AQL-specific elements.
 *
 * @property log Logger instance for diagnostic output.
 */
class AqlSyntaxHighlighter(
    private val log: Logger = LoggerFactory.getLogger(AqlSyntaxHighlighter::class.java)
) : SyntaxHighlighterBase() {

    companion object {
        /** Empty attribute set, used as a fallback when no highlighting is needed. */
        val EMPTY: Array<TextAttributesKey> = arrayOf()

        /** Attribute set for line comments. */
        val LINE_COMMENT: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.LINE_COMMENT)

        /** Attribute set for block comments. */
        val BLOCK_COMMENT: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.BLOCK_COMMENT)

        /** Attribute set for string literals. */
        val STRINGS: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.STRING)

        /** Attribute set for function names. */
        val FUNCTION: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.FUNCTION)

        /** Attribute set for keywords. */
        val KEYWORD: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.KEYWORD)

        /**Attribute set for property lookup tokens (dot notation). */
        val PROPERTY_LOOKUP: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.PROPERTY_LOOKUP)

        /** Attribute set for property names. */
        val PROPERTY_NAME: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.PROPERTY_NAME)

        /** Attribute set for system property tokens. */
        val SYSTEM_PROPERTY: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.SYSTEM_PROPERTY)

        /** Attribute set for variable placeholders. */
        val VARIABLE_PLACE_HOLDER: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.VARIABLE_PLACE_HOLDER)

        /**  Attribute set for parameter variables. */
        val PARAMETER_VARIABLE: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.PARAMETER_VARIABLE)

        /** Attribute set for brackets and braces. */
        val BRACKETS: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.AQL_BRACES)

        /** Attribute set for numeric literals. */
        val NUMBER: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.NUMBER)

        /** Attribute set for variable identifiers. */
        val VARIABLE: Array<TextAttributesKey> = arrayOf(AqlSyntaxColors.VARIABLE)
    }

    /**
     * Returns the lexer used for syntax highlighting of AQL files.
     *
     * @return an instance of [AqlLexerAdapter]
     */
    override fun getHighlightingLexer() = AqlLexerAdapter()

    /**
     * Maps a token type to its corresponding text attribute keys for highlighting.
     *
     * @param type the token type to highlight
     * @return an array of [TextAttributesKey] for the given token type
     */
    override fun getTokenHighlights(type: IElementType): Array<TextAttributesKey> {
        val language: Language = type.language
        if (language != AqlLanguage) return EMPTY

        // Map the AQL token type to the corresponding text attributes key.
        return when (type) {
            AqlTypes.L_COMMENT -> LINE_COMMENT
            AqlTypes.B_COMMENT -> BLOCK_COMMENT
            AqlTypes.TEXT_DOUBLE, AqlTypes.TEXT_SINGLE -> STRINGS
            AqlTypes.PROPERTY_LOOKUP -> PROPERTY_LOOKUP
            AqlTypes.T_OPEN, AqlTypes.T_CLOSE, AqlTypes.T_ARRAY_OPEN, AqlTypes.T_ARRAY_CLOSE -> BRACKETS
            AqlTypes.PROPERTY_NAME -> PROPERTY_NAME
            AqlTypes.VARIABLE_PLACE_HOLDER -> VARIABLE_PLACE_HOLDER
            AqlTypes.PARAMETER_VARIABLE -> PARAMETER_VARIABLE
            AqlTypes.SYSTEM_PROPERTY -> SYSTEM_PROPERTY
            AqlTypes.NAMED_FUNCTIONS -> FUNCTION
            AqlTypes.NAMED_KEYWORD_STATEMENTS -> KEYWORD
            AqlTypes.ID -> VARIABLE
            AqlTypes.NUMBER, AqlTypes.NUMBER_INTEGER -> NUMBER
            else -> {
                // Workaround for dynamically named types.
                val name = type.toString()
                return when {
                    name.startsWith("F_") -> FUNCTION
                    name.startsWith("T_") -> KEYWORD
                    // Issue 5: backtick-quoted identifiers (`collection_name`) highlighted as strings
                    name == "BACKTICK_ID" -> STRINGS
                    else -> EMPTY.also {
                        log.info("settings: {}", type)
                    }
                }
            }
        }
    }
}