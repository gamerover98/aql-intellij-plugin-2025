package com.arangodb.intellij.aql.services

import com.arangodb.intellij.aql.ArangoBundle
import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.ui.renderers.AqlNodeModel
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic

@Service(Service.Level.PROJECT)
class ArangoProjectService(private val project: Project) {

    init {
        thisLogger().info(ArangoBundle.message("projectService", project.name))
    }

    /**
     * Subscribe an event to the project's message bus.
     * @return The [ArangoProjectService] instance for method chaining.
     */
    fun subscribe(topic: Topic<ActionBusEvent>, event: ActionBusEvent) = apply {
        project.messageBus.connect().subscribe(topic, event)
    }

    fun subscribe(topic: Topic<ActionBusEvent>, event: () -> Unit) = apply {
        project.messageBus.connect().subscribe(topic, ActionBusEvent { event() })
    }

    /**
     * Set the active ArangoDB database based on the selected node in the UI.
     * @return The [ArangoProjectService] instance for method chaining.
     */
    fun setActiveDatabase(selectedNode: AqlNodeModel) = apply {
        //TODO: refactor this code by reimplementing the AqlDataService class here.
        AqlDataService.with(project).setActiveDatabase(selectedNode)
    }

    /**
     * Trigger an event to notify that the active database has been set.
     * @return The [ArangoProjectService] instance for method chaining.
     */
    fun triggerSetActiveDatabaseEvent() = apply {
        val topic = ActionBusEvent.AQL_SYSTEM_ACTIVE_DATABASE_SET
        project
            .messageBus
            .syncPublisher(topic)
            .onEvent(ActionEventData().forName(topic.displayName))
    }
}
