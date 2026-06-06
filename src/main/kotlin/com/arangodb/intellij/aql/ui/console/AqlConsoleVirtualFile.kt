package com.arangodb.intellij.aql.ui.console

import com.arangodb.intellij.aql.fileTypes.AqlFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile

/**
 * In-memory virtual file used as a stable handle for the AQL console editor tab.
 *
 * Stored in project user-data (automatically cleared on project disposal) so that
 * [com.intellij.openapi.fileEditor.FileEditorManager.openFile] always re-focuses
 * the same tab rather than opening a duplicate.
 *
 * The VirtualFile carries no actual content — the [AqlConsoleFileEditor] owns a
 * [com.intellij.ui.LanguageTextField] with its own document for AQL editing.
 */
class AqlConsoleVirtualFile private constructor()
    : LightVirtualFile("AQL Console.aql", AqlFileType, "") {

    // The file is purely a tab handle — make it read-only at the VFS level.
    override fun isWritable(): Boolean = false

    companion object {
        private val KEY = Key.create<AqlConsoleVirtualFile>("aql.console.virtual.file")

        /** Returns the singleton console file for [project], creating it if needed. */
        fun getInstance(project: Project): AqlConsoleVirtualFile =
            project.getUserData(KEY)
                ?: AqlConsoleVirtualFile().also { project.putUserData(KEY, it) }
    }
}
