package com.arangodb.intellij.aql.actions

import com.intellij.util.messages.Topic

fun interface ActionBusEvent {

    companion object {
        @JvmField val ACTION_CONSOLE = "AQL.Action.Console"
        @JvmField val AQL_QUERY_RESULT: Topic<ActionBusEvent> = Topic.create("AQL.QueryResult", ActionBusEvent::class.java)
        @JvmField val AQL_QUERY_TREE_CHANGE: Topic<ActionBusEvent> = Topic.create("AQL.QuerySave", ActionBusEvent::class.java)
        @JvmField val AQL_META_RESULT: Topic<ActionBusEvent> = Topic.create("AQL.MetaResult", ActionBusEvent::class.java)
        @JvmField val AQL_GRAPH_RESULT: Topic<ActionBusEvent> = Topic.create("AQL.GraphResult", ActionBusEvent::class.java)
        @JvmField val AQL_SYSTEM_EMPTY_LOG: Topic<ActionBusEvent> = Topic.create("AQL.System.EmptyLog", ActionBusEvent::class.java)
        @JvmField val AQL_SYSTEM_REFRESH_SCHEME: Topic<ActionBusEvent> = Topic.create("AQL.System.RefreshScheme", ActionBusEvent::class.java)
        @JvmField val AQL_SYSTEM_ACTIVE_DATABASE_SET: Topic<ActionBusEvent> = Topic.create("AQL.System.SetDatabase", ActionBusEvent::class.java)
    }

    fun onEvent(data: ActionEventData)
}
