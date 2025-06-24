package com.arangodb.intellij.aql.intentions

import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.lang.AqlLanguage
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nls

/**
 * Intention action for creating a new AQL database connection in the IDE.
 *
 * This class provides an intention that appears when editing AQL files and no valid ArangoDB connection
 * is configured. When invoked, it opens a dialog to set up a new database connection.
 *
 * > Description and template files are located in the
 * > `src/main/resources/intentionDescriptions/AqlCreateConnectionIntention` directory.
 */
class AqlCreateConnectionIntention : IntentionAction {

    /** Returns the text to be displayed for this intention action. */
    @Nls(capitalization = Nls.Capitalization.Sentence)
    override fun getText() = "Create AQL database connection"

    /** Returns the family name for this intention action, used for grouping similar intentions. */
    @Nls(capitalization = Nls.Capitalization.Sentence)
    override fun getFamilyName() = getText()

    /**
     * Checks if the intention is available in the given context.
     *
     * @param project the current project
     * @param editor the editor where the action is invoked
     * @param psiFile the file in which the action is invoked
     * @return true if the intention is available, false otherwise
     */
    override fun isAvailable(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ): Boolean =
        psiFile.language == AqlLanguage
                && !AqlDataService.with(project).hasValidConnection()

    /**
     * Invokes the intention action, opening the dialog to create a new database connection.
     *
     * @param project the current project
     * @param editor the editor where the action is invoked
     * @param psiFile the file in which the action is invoked
     * @throws IncorrectOperationException if the action cannot be performed
     */
    @Throws(IncorrectOperationException::class)
    override fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {
        AqlDataService.with(project).showServerDialog()
    }

    /** Indicates whether this intention action should be executed in a write action. */
    override fun startInWriteAction() = false
}