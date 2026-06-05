package com.arangodb.intellij.aql.ui

import com.arangodb.intellij.aql.model.ArangoDbServer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Transient

@Service(Service.Level.PROJECT)
@State(name = "ArangoDB.DataSource", storages = [Storage("ArangoDB_DataSource.xml")])
class DataWindowState : PersistentStateComponent<ArangoDbServer> {

    private var _state: ArangoDbServer? = null

    @get:Transient
    var isProcessed: Boolean = false

    override fun loadState(state: ArangoDbServer) {
        _state = state
    }

    override fun getState(): ArangoDbServer {
        if (_state == null) _state = ArangoDbServer()
        return _state!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataWindowState) return false
        return state == other._state
    }

    override fun hashCode(): Int = state.hashCode()
}
