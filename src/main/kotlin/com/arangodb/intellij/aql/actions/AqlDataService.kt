package com.arangodb.intellij.aql.actions

import com.arangodb.ArangoDBException
import com.arangodb.entity.CollectionType
import com.arangodb.entity.IndexEntity
import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.exc.AqlDataSourceException
import com.arangodb.intellij.aql.model.AqlQuery
import com.arangodb.intellij.aql.model.ArangoDbDatabase
import com.arangodb.intellij.aql.model.ArangoDbServer
import com.arangodb.intellij.aql.services.ServerListState
import com.arangodb.intellij.aql.ui.DataWindowState
import com.arangodb.intellij.aql.ui.dialogs.AqlServerDialog
import com.arangodb.intellij.aql.ui.renderers.AqlNodeModel
import com.arangodb.intellij.aql.ui.renderers.AqlQueryModel
import com.arangodb.intellij.aql.util.AqlUtils
import com.arangodb.intellij.aql.util.log
import com.arangodb.model.AqlQueryExplainOptions
import com.google.common.base.Strings
import com.intellij.openapi.project.Project
import com.intellij.ui.CheckedTreeNode
import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultTreeModel

class AqlDataService private constructor(private val project: Project) {

    enum class QueryType { QUERY, EXPLAIN_QUERY }

    private val messageBus = project.messageBus
    private val service: AqlDatabaseService = project.getService(AqlDatabaseService::class.java)
    private val stateComponent: DataWindowState = project.getService(DataWindowState::class.java)

    companion object {
        @JvmStatic
        fun with(project: Project): AqlDataService = AqlDataService(project)
    }

    fun updateQuery(query: AqlQuery): AqlDataService {
        val state = stateComponent.state ?: return this
        if (query.name == null) return this
        state.addQuery(query)
        sendEmptyMessage(ActionBusEvent.AQL_QUERY_TREE_CHANGE)
        return this
    }

    fun deleteQuery(name: String) {
        val queries = getQueries()
        if (!queries.containsKey(name)) {
            log.error("No query found for: $name")
        } else {
            val removed = queries.remove(name)
            if (removed != null) {
                log.info("Query: $name deleted")
                sendEmptyMessage(ActionBusEvent.AQL_QUERY_TREE_CHANGE)
            } else {
                log.error("Failed to remove query: $name")
            }
        }
    }

    fun getExistingQueryForValue(value: String): AqlQuery? {
        val our = AqlQuery("", value).also {
            it.hash = AqlUtils.createHash(project, value, emptyMap())
        }
        return getQueries().values.firstOrNull { it == our }
    }

    fun getExistingQueryForName(name: String): AqlQuery? = getQueries()[name]

    fun hasValidSettings(): Boolean {
        val state = stateComponent.state ?: return false
        return !Strings.isNullOrEmpty(state.user)
            && !Strings.isNullOrEmpty(state.password)
            && !Strings.isNullOrEmpty(state.host)
    }

    fun executeQuery(query: String, queryId: String = ""): AqlDataService =
        executeQueryInternal(query, emptyMap(), QueryType.QUERY, queryId)

    fun executeQuery(query: String, bindVars: Map<String, String>, queryId: String = ""): AqlDataService =
        executeQueryInternal(query, AqlUtils.convertValues(bindVars), QueryType.QUERY, queryId)

    fun explainQuery(query: String, queryId: String = ""): AqlDataService =
        executeQueryInternal(query, emptyMap(), QueryType.EXPLAIN_QUERY, queryId)

    fun explainQuery(query: String, bindVars: Map<String, String>, queryId: String = ""): AqlDataService =
        executeQueryInternal(query, AqlUtils.convertValues(bindVars), QueryType.EXPLAIN_QUERY, queryId)

    /**
     * Executes [query] against [databaseName] on the current server, WITHOUT changing the
     * globally-active database. Used when double-clicking a collection from a non-active DB.
     */
    fun executeQueryInContext(query: String, databaseName: String, queryId: String = ""): AqlDataService {
        val state = project.getService(DataWindowState::class.java).state ?: return this
        val queryPlanEvent = messageBus.syncPublisher(ActionBusEvent.AQL_QUERY_RESULT)
        try {
            val db = service.getDatabaseForContext(state, databaseName, project)
            val items = db.query(query, emptyMap<String, Any>(), String::class.java).asListRemaining()
            val result = when {
                items.isEmpty() -> "[]"
                items.size == 1 -> items[0]
                else -> "[${items.joinToString(",")}]"
            }
            val data = ActionEventData(ActionEventData.KEY_QUERY, query)
            data.set(ActionEventData.KEY_RESULT, result)
            if (queryId.isNotBlank()) data.set(ActionEventData.KEY_QUERY_ID, queryId)
            queryPlanEvent.onEvent(data)
        } catch (e: ArangoDBException) {
            val err = ActionEventData(ActionEventData.KEY_RESULT, e.message ?: "ArangoDB error")
            if (queryId.isNotBlank()) err.set(ActionEventData.KEY_QUERY_ID, queryId)
            queryPlanEvent.onEvent(err)
        } catch (e: AqlDataSourceException) {
            val err = ActionEventData(ActionEventData.KEY_RESULT, e.message ?: "Connection error")
            if (queryId.isNotBlank()) err.set(ActionEventData.KEY_QUERY_ID, queryId)
            queryPlanEvent.onEvent(err)
        }
        return this
    }

    fun getFieldNames(collectionName: String): List<String> =
        try { service.getFieldNames(collectionName, project) } catch (_: Exception) { emptyList() }

    private fun executeQueryInternal(query: String, bindVars: Map<String, Any>, type: QueryType,
                                      queryId: String = ""): AqlDataService {
        val state = project.getService(DataWindowState::class.java).state ?: return this
        val queryPlanEvent = messageBus.syncPublisher(ActionBusEvent.AQL_QUERY_RESULT)
        try {
            val activeDatabase = service.getActiveDatabase(state, project)
            val result = if (type == QueryType.QUERY) {
                val items = activeDatabase.query(query, bindVars, String::class.java).asListRemaining()
                when {
                    items.isEmpty() -> "[]"
                    items.size == 1 -> items[0]
                    else -> "[${items.joinToString(",")}]"
                }
            } else {
                val options = AqlQueryExplainOptions()
                val explainEntity = activeDatabase.explainQuery(query, bindVars, options)
                AqlUtils.parseExecutionEntity(explainEntity)
            }
            val data = ActionEventData(ActionEventData.KEY_QUERY, query)
            data.set(ActionEventData.KEY_RESULT, result)
            if (queryId.isNotBlank()) data.set(ActionEventData.KEY_QUERY_ID, queryId)
            queryPlanEvent.onEvent(data)
        } catch (e: ArangoDBException) {
            val err = ActionEventData(ActionEventData.KEY_RESULT, e.message ?: "ArangoDB error")
            if (queryId.isNotBlank()) err.set(ActionEventData.KEY_QUERY_ID, queryId)
            queryPlanEvent.onEvent(err)
        } catch (e: AqlDataSourceException) {
            val err = ActionEventData(ActionEventData.KEY_RESULT, e.message ?: "Connection error")
            if (queryId.isNotBlank()) err.set(ActionEventData.KEY_QUERY_ID, queryId)
            queryPlanEvent.onEvent(err)
        }
        return this
    }

    fun saveQuery(query: AqlQuery): AqlDataService {
        val state = stateComponent.state ?: return this
        if (state.getQueries().containsValue(query)) return this
        val name = AqlUtils.createFileName(query.name ?: "Query", state.getQueries())
        query.name = name
        query.hash = AqlUtils.createHash(project, query.query ?: "", query.getParameters())
        state.addQuery(query)
        sendEmptyMessage(ActionBusEvent.AQL_QUERY_TREE_CHANGE)
        return this
    }

    fun server(): ArangoDbServer =
        if (!hasValidSettings()) stateComponent.state ?: ArangoDbServer()
        else try {
            service.getServer(project)
        } catch (_: AqlDataSourceException) {
            stateComponent.state ?: ArangoDbServer()
        }

    fun sendEmptyMessage(topic: Topic<ActionBusEvent>) {
        messageBus.syncPublisher(topic).onEvent(ActionEventData.EMPTY)
    }

    fun refreshSchema(): AqlDataService {
        if (!hasValidSettings()) {
            AqlUtils.popupDataSourceFix("Setup ArangoDB connection", project)
            return this
        }
        val server = try {
            service.getServer(project)
        } catch (e: AqlDataSourceException) {
            AqlUtils.popupDataSourceFix(e.message ?: "Cannot connect to ArangoDB", project)
            return this
        }
        service.refresh(server, project)
        val event = messageBus.syncPublisher(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME)
        event.onEvent(ActionEventData().forName(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME.displayName))
        return this
    }

    fun setActiveDatabase(): AqlDataService {
        val event = messageBus.syncPublisher(ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET)
        event.onEvent(ActionEventData().forName(ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET.displayName))
        return this
    }

    fun setActiveDatabase(selectedNode: AqlNodeModel): AqlDataService {
        val state = stateComponent.state ?: return this
        val name = selectedNode.name
        state.selectedDatabase = ArangoDbDatabase(name ?: "")
        log.info("New active database: $name")
        return refreshSchema()
    }

    fun subscribe(topic: Topic<ActionBusEvent>, event: ActionBusEvent): AqlDataService {
        messageBus.connect().subscribe(topic, event)
        return this
    }

    fun getQueries(): MutableMap<String, AqlQuery> =
        stateComponent.state?.getQueries()?.toMutableMap() ?: mutableMapOf()

    fun showServerDialog() {
        val state = stateComponent.state ?: return
        val dialog = AqlServerDialog(project)
        dialog.setData(state)
        if (!dialog.showAndGet()) return
        val response = testServerConnection(dialog.getData())
        if (response.isError()) {
            log.error(response.message)
            return
        }
        val newServer = dialog.getData()
        stateComponent.loadState(newServer)
        // C2: keep ServerListState in sync
        val serverList = ServerListState.getInstance(project)
        val existingIdx = serverList.findIndex(state.host, state.port)
        if (existingIdx >= 0) serverList.replaceServer(existingIdx, newServer)
        else serverList.addServer(newServer)
        refreshSchema()
    }

    fun testServerConnection(server: ArangoDbServer): ActionResponse =
        try {
            service.checkServerConnection(server, project)
            ActionResponse.info("OK")
        } catch (e: AqlDataSourceException) {
            ActionResponse.error(e.message ?: "Unknown error")
        }

    fun sendResponse(response: ActionResponse) {
        if (response.isError()) log.error(response.message) else log.info(response.message)
    }

    fun populateQueryTree(): DefaultTreeModel {
        val name = stateComponent.state?.name ?: ""
        val queryModel = AqlQueryModel(name, AqlQueryModel.Type.ROOT)
        val root = CheckedTreeNode(queryModel).also { it.isChecked = true }
        getQueries().forEach { (key, value) ->
            val node = CheckedTreeNode()
            node.userObject = AqlQueryModel(key, value.getParameters(), AqlQueryModel.Type.WITH_PARAMS)
            root.add(node)
        }
        return DefaultTreeModel(root)
    }

    /**
     * Clears the active server settings, removes it from [ServerListState], and fires
     * a schema-refresh event so the tool window reverts to its empty/multi-server state.
     */
    fun removeServer(): AqlDataService {
        val current = stateComponent.state
        ServerListState.getInstance(project).removeByHostPort(current.host, current.port)
        stateComponent.loadState(ArangoDbServer())
        sendEmptyMessage(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME)
        return this
    }

    /**
     * C1: Silent auto-refresh variant — fetches fresh schema and fires the refresh
     * event without showing any error popup on failure.
     * Intended to be called from the background auto-refresh scheduler.
     */
    fun refreshSchemaSilent(): AqlDataService {
        if (!hasValidSettings()) return this
        try {
            val server = service.getServer(project)
            service.refresh(server, project)
            sendEmptyMessage(ActionBusEvent.AQL_SYSTEM_REFRESH_SCHEME)
        } catch (_: AqlDataSourceException) {
            // silent — don't interrupt the user on a background tick
        }
        return this
    }

    /**
     * C3: Returns the list of indexes for [collectionName] in the active database.
     * Returns an empty list on any error (no connection, unknown collection, etc.).
     */
    fun getCollectionIndexes(collectionName: String): List<IndexEntity> =
        try { service.getCollectionIndexes(collectionName, project) } catch (_: Exception) { emptyList() }

    /**
     * C2: Builds a tree with multiple SERVER roots under a single invisible root.
     *
     * [allServers] is the full list from [ServerListState].
     * [activeServer] is the currently connected server with its databases populated —
     *   matched to entries in [allServers] by host+port.
     *   Non-matching entries are shown as empty SERVER nodes (not yet activated).
     *
     * Set [Tree.isRootVisible] = false when using this model.
     */
    fun populateTreeMulti(
        allServers: List<ArangoDbServer>,
        activeServer: ArangoDbServer?,
        includeSystem: Boolean = true
    ): DefaultTreeModel {
        val invisibleRoot = CheckedTreeNode()   // no userObject → invisible root
        for (serverDef in allServers) {
            val isActive = activeServer != null &&
                serverDef.host == activeServer.host && serverDef.port == activeServer.port
            val serverData = if (isActive) activeServer!! else serverDef
            // Re-use the single-server builder and harvest its root node
            val singleModel = populateTree(serverData, includeSystem)
            val serverRoot  = singleModel.root as? CheckedTreeNode ?: continue
            invisibleRoot.add(serverRoot)
        }
        return DefaultTreeModel(invisibleRoot)
    }

    /**
     * Builds the full tree model for [server].
     *
     * A3: Each database shows four optional virtual category folders:
     *   "Collections (N)", "Edge Collections (N)", "Graphs (N)", "Views (N)"
     *   — folders with 0 items are omitted.
     * A4: System collections (`_` prefix) are grouped in a "System (N)" folder
     *   (omitted entirely when [includeSystem] = false, e.g. the B4 toolbar toggle).
     * A6: SERVER and DATABASE nodes carry [AqlNodeModel.tooltipLines] for rich hover tooltips.
     */
    fun populateTree(server: ArangoDbServer, includeSystem: Boolean = true): DefaultTreeModel {
        // A6: SERVER tooltip — host:port, user, SSL, database count
        // C2: tag stores "host:port" for server identification in the context menu
        val serverObject = AqlNodeModel(server.name, server.host, AqlNodeModel.Type.SERVER).also {
            it.tag = "${server.host}:${server.port}"
            it.tooltipLines = buildList {
                add("<b>${server.host}:${server.port}</b>")
                val user = server.user
                if (!user.isNullOrEmpty()) add("User: $user")
                add(if (server.isUseSsl) "SSL: enabled" else "SSL: disabled")
                val dbCount = server.databases.size
                if (dbCount > 0) add("Databases: $dbCount")
            }
        }
        val root = CheckedTreeNode(serverObject).also { it.isChecked = true }
        val selectedDatabase = server.selectedDatabase ?: ArangoDbDatabase()

        for (database in server.databases) {
            val collections   = database.collections ?: emptyList()
            val regularCols   = collections.filter { it.type != CollectionType.EDGES && !(it.name ?: "").startsWith("_") }
            val edgeCols      = collections.filter { it.type == CollectionType.EDGES  && !(it.name ?: "").startsWith("_") }
            val graphs        = database.graphs ?: emptyList()
            val views         = database.views  ?: emptyList()
            val sysCols       = if (includeSystem) collections.filter { (it.name ?: "").startsWith("_") } else emptyList()

            // A6: DATABASE tooltip — name, per-category counts
            val dbObject = AqlNodeModel(database.name, database.name, AqlNodeModel.Type.DATABASE).also {
                if (selectedDatabase == database) it.isSelected = true
                it.tooltipLines = buildList {
                    add("<b>${database.name ?: ""}</b>")
                    if (regularCols.isNotEmpty()) add("Collections: ${regularCols.size}")
                    if (edgeCols.isNotEmpty())    add("Edge Collections: ${edgeCols.size}")
                    if (graphs.isNotEmpty())      add("Graphs: ${graphs.size}")
                    if (views.isNotEmpty())       add("Views: ${views.size}")
                    if (sysCols.isNotEmpty())     add("System: ${sysCols.size}")
                }
            }
            val dbNode = CheckedTreeNode().also { it.userObject = dbObject }
            root.add(dbNode)

            // A3: "Collections (N)" folder
            if (regularCols.isNotEmpty()) {
                val catNode = CheckedTreeNode().also {
                    it.userObject = AqlNodeModel("", "Collections (${regularCols.size})", AqlNodeModel.Type.CATEGORY)
                }
                dbNode.add(catNode)
                for (entity in regularCols) {
                    val entityName = entity.name ?: continue
                    catNode.add(CheckedTreeNode().also {
                        it.userObject = AqlNodeModel("", entityName, AqlNodeModel.Type.COLLECTION)
                    })
                }
            }

            // A3: "Edge Collections (N)" folder
            if (edgeCols.isNotEmpty()) {
                val catNode = CheckedTreeNode().also {
                    it.userObject = AqlNodeModel("", "Edge Collections (${edgeCols.size})", AqlNodeModel.Type.CATEGORY)
                }
                dbNode.add(catNode)
                for (entity in edgeCols) {
                    val entityName = entity.name ?: continue
                    catNode.add(CheckedTreeNode().also {
                        it.userObject = AqlNodeModel("", entityName, AqlNodeModel.Type.EDGE)
                    })
                }
            }

            // A3: "Graphs (N)" folder
            if (graphs.isNotEmpty()) {
                val catNode = CheckedTreeNode().also {
                    it.userObject = AqlNodeModel("", "Graphs (${graphs.size})", AqlNodeModel.Type.CATEGORY)
                }
                dbNode.add(catNode)
                for (entity in graphs) {
                    catNode.add(CheckedTreeNode().also {
                        it.userObject = AqlNodeModel("", entity.name, AqlNodeModel.Type.GRAPH)
                    })
                }
            }

            // A3: "Views (N)" folder
            if (views.isNotEmpty()) {
                val catNode = CheckedTreeNode().also {
                    it.userObject = AqlNodeModel("", "Views (${views.size})", AqlNodeModel.Type.CATEGORY)
                }
                dbNode.add(catNode)
                for (entity in views) {
                    catNode.add(CheckedTreeNode().also {
                        it.userObject = AqlNodeModel("", entity.name, AqlNodeModel.Type.VIEW)
                    })
                }
            }

            // A4: "System (N)" folder — omitted when includeSystem = false
            if (sysCols.isNotEmpty()) {
                val sysNode = CheckedTreeNode().also {
                    it.userObject = AqlNodeModel("", "System (${sysCols.size})", AqlNodeModel.Type.CATEGORY)
                }
                dbNode.add(sysNode)
                for (entity in sysCols) {
                    val entityName = entity.name ?: continue
                    val colType = if (entity.type == CollectionType.EDGES) AqlNodeModel.Type.EDGE
                                  else AqlNodeModel.Type.COLLECTION
                    sysNode.add(CheckedTreeNode().also {
                        it.userObject = AqlNodeModel("", entityName, colType)
                    })
                }
            }
        }
        return DefaultTreeModel(root)
    }

    /** Convenience wrapper used by [com.arangodb.intellij.aql.toolWindow.ServerToolWindow]. */
    fun getCollectionCount(collectionName: String): Long =
        service.getCollectionCount(collectionName, project)

    fun hasValidConnection(): Boolean =
        hasValidSettings() && service.isConnectionValid(project)
}
