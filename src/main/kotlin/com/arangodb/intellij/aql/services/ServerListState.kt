package com.arangodb.intellij.aql.services

import com.arangodb.intellij.aql.model.ArangoDbServer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * C2: Persistent registry of all configured ArangoDB servers.
 *
 * Stored in `.idea/arangodb-servers.xml`.
 *
 * The *active* server (the one used by code completion and query execution) is
 * still tracked by [com.arangodb.intellij.aql.ui.DataWindowState] so that all
 * existing callers continue to work without modification.  This registry only
 * adds multi-server management to the UI layer.
 *
 * ### Migration
 * On the first time this service is accessed with an empty list, call
 * [migrateIfEmpty] passing the legacy [com.arangodb.intellij.aql.ui.DataWindowState]
 * value so that single-server users transparently keep their configuration.
 */
@Service(Service.Level.PROJECT)
@State(name = "ArangoDB.ServerList", storages = [Storage("arangodb-servers.xml")])
class ServerListState : PersistentStateComponent<ServerListState.State> {

    class State {
        var servers: MutableList<ArangoDbServer> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    /** Returns a snapshot of all registered servers (safe to iterate while mutating). */
    fun getServers(): List<ArangoDbServer> = myState.servers.toList()

    /** Appends [server] to the list. */
    fun addServer(server: ArangoDbServer) {
        myState.servers.add(server)
    }

    /**
     * Replaces the server at [index] with [server].
     * No-op when [index] is out of range.
     */
    fun replaceServer(index: Int, server: ArangoDbServer) {
        if (index in myState.servers.indices) myState.servers[index] = server
    }

    /**
     * Removes all entries whose [ArangoDbServer.host] and [ArangoDbServer.port]
     * match the given values.
     */
    fun removeByHostPort(host: String, port: Int) {
        myState.servers.removeAll { it.host == host && it.port == port }
    }

    /**
     * Returns the list index of the first server matching [host]:[port], or −1
     * when not found.
     */
    fun findIndex(host: String, port: Int): Int =
        myState.servers.indexOfFirst { it.host == host && it.port == port }

    /**
     * One-time migration: if the registry is empty and [legacy] has valid settings
     * (non-blank host and a non-null user), adds it as the first entry.
     */
    fun migrateIfEmpty(legacy: ArangoDbServer) {
        if (myState.servers.isEmpty() && legacy.host.isNotBlank() && legacy.user != null) {
            myState.servers.add(legacy)
        }
    }

    companion object {
        fun getInstance(project: Project): ServerListState =
            project.getService(ServerListState::class.java)
    }
}
