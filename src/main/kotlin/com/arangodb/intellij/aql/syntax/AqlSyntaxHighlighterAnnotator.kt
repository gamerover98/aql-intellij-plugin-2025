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

// Semantic highlighter: applies PSI-level colors on top of the lexer-level AqlSyntaxHighlighter.
class AqlSyntaxHighlighterAnnotator(
    private val log: Logger = LoggerFactory.getLogger(AqlSyntaxHighlighterAnnotator::class.java)
) : Annotator {

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
                    else -> log.info("Missing aqlType {}", aqlType)
                }
            }

            is AqlLineComment -> annotate(element, holder, LINE_COMMENT)
            is AqlBlockComment -> annotate(element, holder, BLOCK_COMMENT)
            is PsiWhiteSpace -> {
                val text = element.text
                val idx = text.indexOf("\\n").takeIf { it >= 0 } ?: text.indexOf("\\r")

                if (idx != -1) {
                    element.textRange?.let { textRange ->
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(textRange)
                            .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
                            .textAttributes(ESCAPE_CHARACTERS)
                            .create()
                    }
                }
            }
        }
    }
}

private fun annotate(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(element)
        .textAttributes(key)
        .create()
}
