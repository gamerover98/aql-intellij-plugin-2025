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
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        fun getInstance(project: Project): AqlConsoleStateService =
            project.getService(AqlConsoleStateService::class.java)
    }
}
