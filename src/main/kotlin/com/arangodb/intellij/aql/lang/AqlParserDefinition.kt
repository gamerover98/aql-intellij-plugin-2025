package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.fileTypes.AqlFile
import com.arangodb.intellij.aql.grammar.custom.psi.AqlLexerAdapter
import com.arangodb.intellij.aql.grammar.generated.AqlParser
import com.arangodb.intellij.aql.grammar.generated.psi.AqlTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * This TokenSet is used by the parser to recognize
 * and handle whitespace characters in AQL files.
 *
 * For example, in the code `LET x = 1`, the spaces
 * between `LET`, `x`, `=`, and `1` are considered whitespace.
 */
val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)

/**
 * This TokenSet is used by the parser to recognize
 * and handle comments in AQL files.
 *
 * It includes both single-line comments (e.g. `// comment`)
 * and multi-line comments (e.g. `/* comment */`).
 */
val COMMENTS = TokenSet.create(AqlTypes.COMMENT, AqlTypes.BLOCK_COMMENT)

/**
 * This TokenSet is used by the parser to recognize
 * and handle string literals in AQL files.
 *
 * It includes both single-quoted strings (e.g. `'text'`)
 * and double-quoted strings (e.g. `"text"`).
 */
val STRING_LITERALS = TokenSet.create(AqlTypes.TEXT_SINGLE, AqlTypes.TEXT_DOUBLE)

/**
 * Defines the file element type for AQL files.
 * This constant is used by the IntelliJ Platform to identify and handle
 * the root element of AQL files in the PSI (Program Structure Interface) tree.
 */
private val FILE = IFileElementType(Language.findInstance(AqlLanguage::class.java))

/**
 * Parser definition for the AQL language.
 * Used by the IntelliJ Platform to parse and handle AQL files.
 */
class AqlParserDefinition : ParserDefinition {

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode?, right: ASTNode?
    ): SpaceRequirements {
        return SpaceRequirements.MAY // <-- Whitespace between tokens is optional.
    }

    /** Creates the lexer for the AQL language. */
    override fun createLexer(project: Project?) = AqlLexerAdapter()

    /** Creates the parser for the AQL language. */
    override fun createParser(project: Project?) = AqlParser()

    /** Returns the file element type for AQL files. */
    override fun getFileNodeType() = FILE

    /** Returns a TokenSet containing the whitespace token types of the AQL language */
    override fun getWhitespaceTokens() = WHITE_SPACES

    /** Returns a TokenSet containing the comment token types of the AQL language. */
    override fun getCommentTokens() = COMMENTS

    /** Returns a TokenSet containing the literal token types of the AQL language. */
    override fun getStringLiteralElements() = STRING_LITERALS

    /**
     * Creates a PSI element from the given AST node.
     * This method is used by the IntelliJ Platform to convert AST nodes
     * into PSI elements specific to the AQL language.
     *
     * Example:
     * Given an AST node representing an AQL keyword, this method returns
     * the corresponding PSI element for that keyword, enabling features
     * like syntax highlighting and code navigation.
     */
    override fun createElement(node: ASTNode): PsiElement = AqlTypes.Factory.createElement(node)

    /** Creates a PSI file from the given file view provider. */
    override fun createFile(viewProvider: FileViewProvider) = AqlFile(viewProvider)
}