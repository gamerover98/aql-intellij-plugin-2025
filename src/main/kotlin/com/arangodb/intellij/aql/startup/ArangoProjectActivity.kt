package com.arangodb.intellij.aql.startup

import com.arangodb.intellij.aql.services.AqlConsoleStateService
import com.arangodb.intellij.aql.ui.console.AqlConsoleVirtualFile
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs once per project after the IDE has finished opening.
 *
 * Responsibilities:
 *  1. **Tab restoration** — if the AQL Console was open when the IDE was last closed,
 *     reopen it automatically (without stealing focus from the user's active tab).
 *  2. **Tab tracking** — subscribe to [FileEditorManagerListener] to persist
 *     [AqlConsoleStateService.State.wasConsoleOpen] for the next startup.
 *
 * The flag is `false` by default, so on a fresh install the console is not opened
 * automatically; the user opens it once, and from then on it persists across restarts.
 * If the user explicitly closes the tab before quitting, it will NOT be reopened.
 */
class ArangoProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // ── 1. Restore the console tab if it was open in the previous session ──────
        if (AqlConsoleStateService.getInstance(project).state.wasConsoleOpen) {
            withContext(Dispatchers.EDT) {
                FileEditorManager.getInstance(project).openFile(
                    AqlConsoleVirtualFile.getInstance(project),
                    false   // focusEditor = false: don't steal focus from the last active tab
                )
            }
        }

        // ── 2. Track open / close for future sessions ─────────────────────────────
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    if (file is AqlConsoleVirtualFile)
                        AqlConsoleStateService.getInstance(project).state.wasConsoleOpen = true
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    if (file is AqlConsoleVirtualFile)
                        AqlConsoleStateService.getInstance(project).state.wasConsoleOpen = false
                }
            }
        )
    }
}
