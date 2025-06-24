package com.arangodb.intellij.aql.syntax

import com.arangodb.intellij.aql.lang.AqlSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Factory for providing the AQL syntax highlighter in the IntelliJ Platform.
 *
 * The IDE uses this class to supply a custom syntax highlighter for AQL files,
 * enabling syntax coloring and improved code readability for ArangoDB Query Language.
 */
class AqlSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    /**
     * Returns an instance of [AqlSyntaxHighlighter] for the given project and file.
     *
     * @param project The current project (could be null).
     * @param virtualFile The file to highlight (could be null).
     * @return A [SyntaxHighlighter] for AQL files.
     */
    override fun getSyntaxHighlighter(
        project: Project?,
        virtualFile: VirtualFile?
    ): SyntaxHighlighter = AqlSyntaxHighlighter()
}