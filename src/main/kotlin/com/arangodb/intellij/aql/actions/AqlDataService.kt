package com.arangodb.intellij.aql.actions

import com.arangodb.ArangoDBException
import com.arangodb.entity.CollectionType
import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.exc.AqlDataSourceException
import com.arangodb.intellij.aql.model.AqlQuery
import com.arangodb.intellij.aql.model.ArangoDbDatabase
import com.arangodb.intellij.aql.model.ArangoDbServer
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

    fun executeQuery(query: String): AqlDataService =
        executeQueryInternal(query, emptyMap(), QueryType.QUERY)

    fun executeQuery(query: String, bindVars: Map<String, String>): AqlDataService =
        executeQueryInternal(query, AqlUtils.convertValues(bindVars), QueryType.QUERY)

    fun explainQuery(query: String): AqlDataService =
        executeQueryInternal(query, emptyMap(), QueryType.EXPLAIN_QUERY)

    fun explainQuery(query: String, bindVars: Map<String, String>): AqlDataService =
        executeQueryInternal(query, AqlUtils.convertValues(bindVars), QueryType.EXPLAIN_QUERY)

    private fun executeQueryInternal(query: String, bindVars: Map<String, Any>, type: QueryType): AqlDataService {
        val state = project.getService(DataWindowState::class.java).state ?: return this
        val queryPlanEvent = messageBus.syncPublisher(ActionBusEvent.AQL_QUERY_RESULT)
        try {
            val activeDatabase = service.getActiveDatabase(state, project)
            val result = if (type == QueryType.QUERY) {
                val cursor = activeDatabase.query(query, bindVars, String::class.java)
                cursor.asListRemaining().joinToString("")
            } else {
                val options = AqlQueryExplainOptions()
                val explainEntity = activeDatabase.explainQuery(query, bindVars, options)
                AqlUtils.parseExecutionEntity(explainEntity)
            }
            val data = ActionEventData(ActionEventData.KEY_QUERY, query)
            data.set(ActionEventData.KEY_RESULT, result)
            queryPlanEvent.onEvent(data)
        } catch (e: ArangoDBException) {
            queryPlanEvent.onEvent(ActionEventData(ActionEventData.KEY_RESULT, e.message))
        } catch (e: AqlDataSourceException) {
            queryPlanEvent.onEvent(ActionEventData(ActionEventData.KEY_RESULT, e.message))
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
        stateComponent.loadState(dialog.getData())
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

    fun populateTree(server: ArangoDbServer): DefaultTreeModel {
        val serverObject = AqlNodeModel(server.name, server.host, AqlNodeModel.Type.SERVER)
        val root = CheckedTreeNode(serverObject).also { it.isChecked = true }
        val selectedDatabase = server.selectedDatabase ?: ArangoDbDatabase()

        for (database in server.databases) {
            val dbObject = AqlNodeModel(database.name, database.name, AqlNodeModel.Type.DATABASE).also {
                if (selectedDatabase == database) it.isSelected = true
            }
            val dbNode = CheckedTreeNode().also { it.userObject = dbObject }
            root.add(dbNode)

            val collections = database.collections ?: emptyList()

            for (entity in collections) {
                if (entity.type == CollectionType.EDGES) continue
                val entityName = entity.name ?: continue
                if (entityName.startsWith("_")) continue
                val node = CheckedTreeNode()
                node.userObject = AqlNodeModel("", entityName, AqlNodeModel.Type.COLLECTION)
                dbNode.add(node)
            }
            for (entity in collections) {
                if (entity.type != CollectionType.EDGES) continue
                val entityName = entity.name ?: continue
                if (entityName.startsWith("_")) continue
                val node = CheckedTreeNode()
                node.userObject = AqlNodeModel("", entityName, AqlNodeModel.Type.COLLECTION)
                dbNode.add(node)
            }
            for (entity in database.graphs ?: emptyList()) {
                val node = CheckedTreeNode()
                node.userObject = AqlNodeModel("", entity.name, AqlNodeModel.Type.GRAPH)
                dbNode.add(node)
            }
            for (entity in database.views ?: emptyList()) {
                val node = CheckedTreeNode()
                node.userObject = AqlNodeModel("", entity.name, AqlNodeModel.Type.VIEW)
                dbNode.add(node)
            }
            for (entity in collections) {
                val entityName = entity.name ?: continue
                if (!entityName.startsWith("_")) continue
                val node = CheckedTreeNode()
                val collectionType = if (entity.type == CollectionType.EDGES) AqlNodeModel.Type.EDGE else AqlNodeModel.Type.COLLECTION
                node.userObject = AqlNodeModel("", entityName, collectionType)
                dbNode.add(node)
            }
        }
        return DefaultTreeModel(root)
    }

    fun hasValidConnection(): Boolean =
        hasValidSettings() && service.isConnectionValid(project)
}
