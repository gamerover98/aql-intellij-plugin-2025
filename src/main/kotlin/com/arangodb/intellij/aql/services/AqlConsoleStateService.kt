package com.arangodb.intellij.aql.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * Per-project persistent state for the AQL Console tool window.
 *
 * Survives IDE restarts — stored in `.idea/aql-console.xml`.
 * Holds:
 *  - [State.editorText] — the current AQL query text in the editor
 *  - [State.history]    — up to 200 most-recent query history entries (newest first)
 *  - [State.lastResult] — the raw JSON of the last executed query (capped at 200 000 chars)
 */
@Service(Service.Level.PROJECT)
@State(
    name = "AqlConsoleState",
    storages = [Storage("aql-console.xml")]
)
class AqlConsoleStateService : PersistentStateComponent<AqlConsoleStateService.State> {

    /**
     * A single query-history entry.
     * Must have a no-arg constructor (provided by default-argument data class) for IntelliJ xmlb.
     */
    data class HistoryItem(
        var timestamp: String = "",
        var query: String = ""
    )

    class State {
        var editorText: String = ""
        var history: MutableList<HistoryItem> = mutableListOf()
        var lastResult: String = ""
        /**
         * B5: Stable expansion paths for the ArangoDB connection tree.
         * Each entry is the node path from root to a previously expanded node,
         * with path components joined by "›" and count suffixes (e.g. " (5)")
         * stripped so the saved state survives schema changes.
         */
        var treeExpandedPaths: MutableList<String> = mutableListOf()
        /**
         * True while the AQL Console tab is open.
         * Persisted so [ArangoProjectActivity] can reopen the tab on the next IDE startup.
         * Defaults to false — the console is not opened automatically on first install.
         */
        var wasConsoleOpen: Boolean = false
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        fun getInstance(project: Project): AqlConsoleStateService =
            project.getService(AqlConsoleStateService::class.java)
    }
}
