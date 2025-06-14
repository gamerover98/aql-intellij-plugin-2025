package com.arangodb.intellij.aql.file

import com.arangodb.intellij.aql.ArangoBundle
import com.arangodb.intellij.aql.fileTypes.AqlFile
import com.arangodb.intellij.aql.util.Icons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

private val ACTION_TEXT = ArangoBundle.message("AqlFileTemplateAction.text")
private val ACTION_DESCRIPTION = ArangoBundle.message("AqlFileTemplateAction.description")

/**
 * Action for creating a new AQL file from a template in IntelliJ IDEA.
 *
 * This class integrates with the IDE's file creation dialog, allowing users to generate
 * new ArangoDB Query Language (AQL) files using a predefined template. It also handles
 * post-processing, such as moving the caret to the end of the newly created file.
 */
class AqlFileTemplateAction :
    CreateFileFromTemplateAction(
        ACTION_TEXT,
        ACTION_DESCRIPTION,
        Icons.ICON_ARANGO
    ), DumbAware {

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder
            .setTitle(ACTION_TEXT)
            .addKind(
                ACTION_TEXT,
                Icons.ICON_ARANGO,
                "AQL Template" // Template file name
            )
    }

    /**
     * Returns the action name to be displayed when creating a new AQL file from a template.
     *
     * @param directory The target directory where the file will be created.
     * @param newName The name of the new file.
     * @param templateName The name of the template used.
     * @return The action name as a string.
     */
    override fun getActionName(
        directory: PsiDirectory,
        newName: String,
        templateName: String
    ): String = ACTION_TEXT

    /**
     * Post-processes the newly created AQL file after it has been generated from a template.
     *
     * Moves the caret to the end of the file if the created element is an AqlFile and the editor is available.
     * If the created element is not an AqlFile, shows an informational notification.
     *
     * @param createdElement The file created from the template.
     * @param templateName The name of the template used.
     * @param customProperties Custom properties for the template.
     */
    override fun postProcess(
        createdElement: PsiFile,
        templateName: String,
        customProperties: Map<String, String>
    ) {
        if (createdElement !is AqlFile) {
            // Notify if the created element is not an AqlFile.
            ArangoBundle.notifyInfo(
                "AqlFileTemplateAction.postProcess.error",
                createdElement.name
            )
            return
        }

        val project = createdElement.project
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val virtualFile = createdElement.containingFile.virtualFile ?: return
        val lastChild = createdElement.lastChild

        // INFO: The caret is the blinking cursor indicating the current text
        //       insertion point in the editor. In this action, the caret is
        //       moved to the end of the newly created file to allow the user
        //       to start editing immediately.
        // Move caret to the end of the file if the document matches the editor's document.
        if (lastChild != null &&
            editor.document == FileDocumentManager.getInstance().getDocument(virtualFile)
        ) {
            editor.caretModel.moveToOffset(lastChild.textRange.endOffset)
        }
    }
}

