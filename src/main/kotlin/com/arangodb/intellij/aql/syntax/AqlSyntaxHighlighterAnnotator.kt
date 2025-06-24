package com.arangodb.intellij.aql.syntax

import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.grammar.generated.psi.AqlBlockComment
import com.arangodb.intellij.aql.grammar.generated.psi.AqlLineComment
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.BLOCK_COMMENT
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.ESCAPE_CHARACTERS
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.FUNCTION
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.KEYWORD
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.LINE_COMMENT
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.PARAMETER_VARIABLE
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.PROPERTY_LOOKUP
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.SYSTEM_PROPERTY
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.VARIABLE
import com.arangodb.intellij.aql.lang.AqlSyntaxColors.VARIABLE_PLACE_HOLDER
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Annotator for AQL syntax highlighting in the IntelliJ editor.
 *
 * This class applies custom syntax highlighting to AQL elements by analyzing PSI elements
 * and assigning appropriate text attributes for keywords, functions, variables, comments,
 * and escape characters. It helps provide visual cues to users editing AQL code,
 * improving readability and code comprehension.
 *
 * @property log Logger instance for reporting missing or unhandled AQL types.
 */
class AqlSyntaxHighlighterAnnotator(
    private val log: Logger = LoggerFactory.getLogger(AqlSyntaxHighlighterAnnotator::class.java)
) : Annotator {

    /**
     * Annotates the given PSI element with syntax highlighting attributes based on its type.
     *
     * This method checks the type of the PSI element and applies the corresponding
     * text attributes for syntax highlighting. It handles named AQL elements, comments,
     * and escape characters in whitespace. Unhandled types are logged for further analysis.
     *
     * @param element The PSI element to annotate.
     * @param holder The annotation holder used to apply highlighting.
     */
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is AqlNamedElement -> {
                when (val aqlType = element.aqlType) {
                    AqlMixinType.KEYWORD -> annotate(element, holder, KEYWORD)
                    AqlMixinType.SYSTEM_PROPERTY -> annotate(element, holder, SYSTEM_PROPERTY)
                    AqlMixinType.FUNCTION -> annotate(element, holder, FUNCTION)
                    AqlMixinType.VAR_PLACEHOLDER -> annotate(element, holder, VARIABLE_PLACE_HOLDER)
                    AqlMixinType.PROPERTY_LOOKUP -> annotate(element, holder, PROPERTY_LOOKUP)
                    AqlMixinType.VAR_PARAMETER -> annotate(element, holder, PARAMETER_VARIABLE)
                    AqlMixinType.ID -> annotate(element, holder, VARIABLE)
                    else -> {
                        log.info("Missing aqlType {}", aqlType)
                    }
                }
            }

            is AqlLineComment -> annotate(element, holder, LINE_COMMENT)
            is AqlBlockComment -> annotate(element, holder, BLOCK_COMMENT)
            is PsiWhiteSpace -> {
                val text = element.text
                val idx = text.indexOf("\\n").takeIf { it >= 0 } ?: text.indexOf("\\r")

                if (idx != -1) {
                    element.textRange?.let { textRange ->
                        val description = ESCAPE_CHARACTERS.externalName
                        holder.newAnnotation(HighlightSeverity.WARNING, description)
                            .range(textRange)
                            .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
                            .textAttributes(ESCAPE_CHARACTERS)
                            .create()
                    }
                }
            }
        }

        //TODO: understand why this code is commented out.
        //when (element) {
        //    is AqlPropertyLookup -> annotate(element, holder, AqlSyntaxColors.PROPERTY_LOOKUP)
        //    is AqlKeywordFunctions -> annotate(element, holder, AqlSyntaxColors.FUNCTION)
        //    is AqlIntegerType -> annotate(element, holder, AqlSyntaxColors.NUMBER)
        //    is AqlParameterVariable -> annotate(element, holder, AqlSyntaxColors.PARAMETER_VARIABLE)
        //    is AqlKeywordStatements -> annotate(element, holder, AqlSyntaxColors.KEYWORD)
        //    is AqlVariablePlaceHolder -> annotate(element, holder, AqlSyntaxColors.VARIABLE_PLACE_HOLDER)
        //    is AqlPropertyName -> annotate(element, holder, AqlSyntaxColors.PROPERTY_NAME)
        //    is AqlLineComment -> annotate(element, holder, AqlSyntaxColors.LINE_COMMENT)
        //    is AqlBlockComment -> annotate(element, holder, AqlSyntaxColors.BLOCK_COMMENT)
        //}
    }
}

//TODO: understand why this code is commented out.
private fun annotate(
    element: PsiElement,
    holder: AnnotationHolder,
    key: TextAttributesKey
) {
    //holder
    //    .newAnnotation(HighlightSeverity.WARNING, key.externalName)
    //    .range(element)
    //    .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
    //    .textAttributes(key)
    //    .create();
}