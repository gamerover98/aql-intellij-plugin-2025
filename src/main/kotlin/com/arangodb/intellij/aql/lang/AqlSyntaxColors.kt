package com.arangodb.intellij.aql.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

/**
 * Global variables that define the syntax highlighting styles
 * for AQL integration with the IntelliJ IDEA editor.
 *
 * Each variable represents a text attribute key used to color specific elements
 * of the AQL language (comments, keywords, variables, functions, etc.).
 */
object AqlSyntaxColors {
    //@formatter:off
    val LINE_COMMENT          = createTextAttributesKey("AQL_LINE_COMMENT",          DefaultLanguageHighlighterColors.LINE_COMMENT)
    val BLOCK_COMMENT         = createTextAttributesKey("AQL_BLOCK_COMMENT",         DefaultLanguageHighlighterColors.BLOCK_COMMENT)
    val KEYWORD               = createTextAttributesKey("AQL_KEYWORD",               DefaultLanguageHighlighterColors.KEYWORD)
    val PROPERTY_LOOKUP       = createTextAttributesKey("AQL_PROPERTY_LOOKUP",       METADATA)
    val PROPERTY_NAME         = createTextAttributesKey("AQL_PROPERTY_NAME",         LOCAL_VARIABLE)
    val SYSTEM_PROPERTY       = createTextAttributesKey("AQL_SYSTEM_PROPERTY",       GLOBAL_VARIABLE)
    val PARAMETER_VARIABLE    = createTextAttributesKey("AQL_PARAMETER_VARIABLE",    INSTANCE_FIELD)
    val VARIABLE_PLACE_HOLDER = createTextAttributesKey("AQL_VARIABLE_PLACE_HOLDER", PARAMETER)
    val FUNCTION              = createTextAttributesKey("AQL_FUNCTION",              FUNCTION_DECLARATION)
    val STRING                = createTextAttributesKey("AQL_STRING",                DefaultLanguageHighlighterColors.STRING)
    val NUMBER                = createTextAttributesKey("AQL_NUMBER",                DefaultLanguageHighlighterColors.NUMBER)
    val OPERATION_SIGN        = createTextAttributesKey("AQL_OPERATION_SIGN",        DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val PARENTHESES           = createTextAttributesKey("AQL_PARENTHESES",           DefaultLanguageHighlighterColors.PARENTHESES)
    val AQL_BRACES            = createTextAttributesKey("AQL_CURLY_BRACES",          BRACKETS)
    val SQUARE_BRACES         = createTextAttributesKey("AQL_SQUARE_BRACES",         BRACKETS)
    val COMMA                 = createTextAttributesKey("AQL_COMMA",                 DefaultLanguageHighlighterColors.COMMA)
    val DOT                   = createTextAttributesKey("AQL_DOT",                   DefaultLanguageHighlighterColors.DOT)

    val VARIABLE          = createTextAttributesKey("AQL_VARIABLE",          createTextAttributesKey("DEFAULT_VARIABLE"))
    val ESCAPE_CHARACTERS = createTextAttributesKey("AQL_ESCAPE_CHARACTERS", createTextAttributesKey("ESCAPE_CHARACTERS"))
    //@formatter:on
}

