package com.arangodb.intellij.aql.actions

class ActionEventData @JvmOverloads constructor(key: String? = null, value: String? = null) {

    companion object {
        @JvmField val EMPTY = ActionEventData()
        const val KEY_QUERY = "query"
        const val KEY_EVENT_NAME = "name"
        const val KEY_RESULT = "result"
        /** Unique ID linking a query execution to its result tab. */
        const val KEY_QUERY_ID = "queryId"
    }

    private val data: MutableMap<String, String> = HashMap()

    init {
        if (key != null && value != null) data[key] = value
    }

    fun forName(value: String): ActionEventData {
        data[KEY_EVENT_NAME] = value
        return this
    }

    fun set(key: String, value: String): ActionEventData {
        data[key] = value
        return this
    }

    fun get(key: String): String? = data[key]
}
