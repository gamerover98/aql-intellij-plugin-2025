package com.arangodb.intellij.aql.model

import com.arangodb.intellij.aql.ui.PasswordManager
import com.intellij.util.xmlb.annotations.Transient

class ArangoDbServer {
    companion object {
        const val DEFAULT_PORT = 8529
    }

    var port: Int = DEFAULT_PORT
    var isExcludeSystemCollections: Boolean = true
    var isUseSsl: Boolean = false
    var host: String = "127.0.0.1"
    var user: String? = null
    var name: String? = null

    // Backing field for selectedDatabaseName — lazily derived from selectedDatabase when null
    private var _selectedDatabaseName: String? = null

    var selectedDatabaseName: String?
        get() = _selectedDatabaseName ?: (_selectedDatabase?.name ?: "").also { _selectedDatabaseName = it }
        set(value) { _selectedDatabaseName = value }

    @field:Transient
    private var _password: String? = null

    @field:Transient
    private var _databases: MutableSet<ArangoDbDatabase>? = null

    @field:Transient
    private var _selectedDatabase: ArangoDbDatabase? = null

    private var _queries: MutableMap<String, AqlQuery>? = null

    constructor()

    constructor(user: String, password: String) {
        _password = password
        this.user = user
    }

    constructor(user: String, password: String, host: String) {
        _password = password
        this.user = user
        this.host = host
        PasswordManager.save(user + host, password)
    }

    @get:Transient
    @set:Transient
    var password: String?
        get() {
            if (_password == null) _password = PasswordManager.load(user + host)
            return _password
        }
        set(value) {
            _password = value
            PasswordManager.save(user + host, value ?: "")
        }

    @get:Transient
    var databases: MutableSet<ArangoDbDatabase>
        get() {
            if (_databases == null) _databases = HashSet()
            return _databases!!
        }
        set(value) { _databases = value }

    @get:Transient
    var selectedDatabase: ArangoDbDatabase?
        get() = _selectedDatabase
        set(value) {
            _selectedDatabase = value
            _selectedDatabaseName = value?.name ?: ""
        }

    fun addDatabase(database: ArangoDbDatabase) {
        databases.add(database)
    }

    fun addQuery(query: AqlQuery) {
        _queries = _queries ?: HashMap()
        _queries!![query.name!!] = query
    }

    fun getQueries(): Map<String, AqlQuery> = _queries ?: HashMap()

    fun setQueries(queries: MutableMap<String, AqlQuery>) {
        _queries = queries
    }
}
