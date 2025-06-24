package com.arangodb.intellij.aql.syntax

import com.intellij.lang.Commenter

/**
 * Implements the [Commenter] interface for AQL (ArangoDB Query Language) files.
 * Provides the line and block comment prefixes and suffixes used by the IDE
 * to comment and uncomment code in AQL files.
 */
class AqlCommenter : Commenter {

    /**
     * Returns the prefix for line comments in AQL.
     * In AQL, line comments start with `//`.
     */
    override fun getLineCommentPrefix() = "//"

    /**
     * Returns the prefix for block comments in AQL.
     * In AQL, block comments start with the `BLOCK_COMMENT_PREFIX`.
     */
    override fun getBlockCommentPrefix() = "/*"

    /**
     * Returns the suffix for block comments in AQL.
     * In AQL, block comments end with BLOCK_COMMENT_SUFFIX.
     */
    override fun getBlockCommentSuffix() = "*/"

    /**
     * Returns the prefix for a commented block comment.
     * Not supported in AQL, so returns null.
     */
    override fun getCommentedBlockCommentPrefix() = null

    /**
     * Returns the suffix for a commented block comment.
     * Not supported in AQL, so returns null.
     */
    override fun getCommentedBlockCommentSuffix() = null
}