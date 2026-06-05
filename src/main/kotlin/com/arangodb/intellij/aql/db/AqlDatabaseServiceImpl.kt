package com.arangodb.intellij.aql.db

import com.arangodb.ArangoDB
import com.arangodb.ArangoDBException
import com.arangodb.ArangoDatabase
import com.arangodb.Protocol
import com.arangodb.entity.CollectionType
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.editor.AqlKeywordElement
import com.arangodb.intellij.aql.exc.AqlDataSourceException
import com.arangodb.intellij.aql.model.ArangoDbDatabase
import com.arangodb.intellij.aql.model.ArangoDbServer
import com.arangodb.intellij.aql.ui.DataWindowState
import com.arangodb.intellij.aql.util.AqlUtils
import com.arangodb.intellij.aql.util.Icons
import com.arangodb.intellij.aql.util.log
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import java.util.HashSet
import java.util.concurrent.TimeUnit

class AqlDatabaseServiceImpl : AqlDatabaseService {

    companion object {
        const val KEY_COLLECTIONS = "collections"
        const val KEY_VIEWS = "views"
        const val KEY_GRAPHS = "graph"
    }

    private val collectionsCache: Cache<String, Collection<LookupElement>> = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.DAYS)
        .maximumSize(10)
        .build()

    // Maps collectionName → sampled field names. TTL 30 min to stay fresh on schema evolution.
    private val fieldNamesCache: Cache<String, List<String>> = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(50)
        .build()

    override fun getAll(): Collection<LookupElement> {
        val all = mutableListOf<LookupElement>()
        all.addAll(getCollections())
        all.addAll(getSearchViews())
        all.addAll(getGraphs())
        return all
    }

    override fun getSearchViews(): Collection<LookupElement> = getLookupElements(KEY_VIEWS)

    override fun getCollections(): Collection<LookupElement> = getLookupElements(KEY_COLLECTIONS)

    override fun getGraphs(): Collection<LookupElement> = getLookupElements(KEY_GRAPHS)

    private fun getLookupElements(key: String): Collection<LookupElement> =
        collectionsCache.getIfPresent(key) ?: emptyList()

    override fun refresh(databaseSettings: ArangoDbServer, project: Project) {
        try {
            collectionsCache.cleanUp()
            val db = getActiveDatabase(databaseSettings, project)
            val collections = db.collections
            val views = db.views
            val graphs = db.graphs

            val viewSet = HashSet<LookupElement>()
            for (view in views) {
                viewSet.add(AqlKeywordElement(view.name, Icons.ICON_VIEW).createLookupElement())
            }
            collectionsCache.put(KEY_VIEWS, viewSet)

            val collectionSet = HashSet<LookupElement>()
            for (entity in collections) {
                val icon = if (entity.type == CollectionType.EDGES) Icons.ICON_EDGE else Icons.ICON_COLLECTION
                val name = entity.name
                if (databaseSettings.isExcludeSystemCollections && name.startsWith("_")) continue
                collectionSet.add(AqlKeywordElement(name, icon).createLookupElement())
            }
            collectionsCache.put(KEY_COLLECTIONS, collectionSet)

            val graphSet = HashSet<LookupElement>()
            for (graph in graphs) {
                graphSet.add(AqlKeywordElement(graph.name, Icons.ICON_GRAPH).createLookupElement())
            }
            collectionsCache.put(KEY_GRAPHS, graphSet)
            log.info("Successfully refreshed ArangoDB database:", databaseSettings.name ?: "")
        } catch (e: ArangoDBException) {
            val errorNum = e.errorNum ?: -1
            if (errorNum != 11) {
                AqlUtils.popupDataSourceFix(e.errorMessage, project)
                return
            }
            log.error("Invalid ArangoDB datasource", e.message ?: "")
        } catch (e: AqlDataSourceException) {
            // checkEmpty() throws with message — show fix-link notification once
            AqlUtils.popupDataSourceFix(e.message ?: "Invalid ArangoDB data source", project)
        }
    }

    override fun getFieldNames(collectionName: String, project: Project): List<String> {
        fieldNamesCache.getIfPresent(collectionName)?.let { return it }
        return try {
            val state = project.getService(com.arangodb.intellij.aql.ui.DataWindowState::class.java).state
                ?: return emptyList()
            val db = getActiveDatabase(state, project)
            // Sample up to 100 documents and collect unique top-level attribute names
            val cursor = db.query(
                "FOR doc IN `$collectionName` LIMIT 100 RETURN KEYS(doc, false)",
                String::class.java
            )
            val fields = mutableSetOf<String>()
            cursor.asListRemaining().forEach { raw ->
                // raw is a JSON array string like ["_id","_key","name","age"]
                val cleaned = raw.trimStart('[').trimEnd(']')
                cleaned.split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                    .forEach { fields.add(it) }
            }
            val sorted = fields.sorted()
            fieldNamesCache.put(collectionName, sorted)
            sorted
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Silent connection probe: no notifications, no heavy schema fetch — used by isAvailable checks
    override fun isConnectionValid(project: Project): Boolean {
        val server = project.getService(DataWindowState::class.java).state ?: return false
        return try {
            checkServerConnection(server, project)
            true
        } catch (_: AqlDataSourceException) {
            false
        }
    }

    // Fetches and populates the full server model (databases, schema).
    // Throws AqlDataSourceException on connection failure — callers are responsible for notifications.
    @Throws(AqlDataSourceException::class)
    override fun getServer(project: Project): ArangoDbServer {
        val stateComponent = project.getService(DataWindowState::class.java)
        val server = stateComponent.state ?: return ArangoDbServer()
        val dataService = AqlDataService.with(project)
        val response = dataService.testServerConnection(server)
        if (response.isError()) {
            throw AqlDataSourceException(response.message)
        }
        try {
            val selectedDatabaseName = server.selectedDatabaseName
            server.databases = HashSet()
            server.selectedDatabase = null
            val databases = getDatabase(server).accessibleDatabases
            for (d in databases) {
                if (server.isExcludeSystemCollections && d.startsWith("_")) continue
                val arangoDbDatabase = ArangoDbDatabase(d)
                populateSchema(server, arangoDbDatabase)
                server.addDatabase(arangoDbDatabase)
                if (selectedDatabaseName == d) {
                    server.selectedDatabase = arangoDbDatabase
                }
                if (server.selectedDatabase == null) {
                    server.selectedDatabase = arangoDbDatabase
                }
            }
        } catch (e: AqlDataSourceException) {
            throw e
        } catch (e: Exception) {
            throw AqlDataSourceException(e)
        }
        return server
    }

    private fun populateSchema(server: ArangoDbServer, database: ArangoDbDatabase) {
        val db = getDatabaseForName(server, database.name ?: "")
        database.collections = db.collections
        database.views = db.views
        database.graphs = db.graphs
    }

    @Throws(AqlDataSourceException::class)
    override fun checkServerConnection(settings: ArangoDbServer, project: Project): ArangoDatabase {
        val user = settings.user ?: throw AqlDataSourceException("No user defined")
        return try {
            val db = ArangoDB.Builder()
                .host(settings.host, settings.port)
                .user(user)
                .useProtocol(Protocol.HTTP_JSON)
                .password(settings.password)
                .useSsl(settings.isUseSsl)
                .build().db()
            db.getPermissions(user)
            db
        } catch (e: Exception) {
            throw AqlDataSourceException(e)
        }
    }

    @Throws(AqlDataSourceException::class)
    override fun getDatabase(settings: ArangoDbServer): ArangoDatabase =
        try {
            ArangoDB.Builder()
                .host(settings.host, settings.port)
                .user(settings.user)
                .useProtocol(Protocol.HTTP_JSON)
                .password(settings.password)
                .build().db()
        } catch (e: Exception) {
            throw AqlDataSourceException(e)
        }

    @Throws(AqlDataSourceException::class)
    override fun getActiveDatabase(settings: ArangoDbServer, project: Project): ArangoDatabase =
        try {
            val user = settings.user
            val selectedDatabase = settings.selectedDatabase
            val database = selectedDatabase?.name
            checkEmpty("user", user)
            checkEmpty("database", database)
            getDatabaseForName(settings, database!!)
        } catch (e: AqlDataSourceException) {
            throw e
        } catch (e: Exception) {
            throw AqlDataSourceException(e)
        }

    private fun getDatabaseForName(settings: ArangoDbServer, database: String): ArangoDatabase =
        ArangoDB.Builder()
            .host(settings.host, settings.port)
            .user(settings.user)
            .useSsl(settings.isUseSsl)
            .useProtocol(Protocol.HTTP_JSON)
            .password(settings.password)
            .build()
            .db(database)

    @Throws(AqlDataSourceException::class)
    private fun checkEmpty(name: String, value: String?) {
        if (value.isNullOrBlank()) throw AqlDataSourceException("No $name set for ArangoDB data source")
    }
}
