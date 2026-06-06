package com.arangodb.intellij.aql.ui.console

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Provides the [AqlConsoleFileEditor] for [AqlConsoleVirtualFile] instances.
 *
 * Registered in plugin.xml as a `fileEditorProvider` extension.
 * [FileEditorPolicy.HIDE_DEFAULT_EDITOR] prevents the plain-text editor from
 * opening alongside our custom editor.
 */
class AqlConsoleEditorProvider : FileEditorProvider, DumbAware {

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean =
        file is AqlConsoleVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        AqlConsoleFileEditor(project, file)

    companion object {
        const val EDITOR_TYPE_ID = "AQL_CONSOLE_EDITOR"
    }
}
