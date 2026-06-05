package com.arangodb.intellij.aql.model

class AqlQuery {
    var name: String? = null
    var query: String? = null
    var hash: String? = null
    private var parameters: MutableMap<String, String>? = null

    constructor()

    constructor(name: String, query: String) {
        this.name = name
        this.query = query
    }

    constructor(name: String, query: String, parameters: MutableMap<String, String>) {
        this.name = name
        this.query = query
        this.parameters = parameters
    }

    fun addParameter(key: String, value: String) {
        if (parameters == null) parameters = HashMap()
        parameters!![key] = value
    }

    fun getParameters(): Map<String, String> = parameters ?: HashMap()

    fun setParameters(parameters: MutableMap<String, String>) {
        this.parameters = parameters
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AqlQuery) return false
        return hash == other.hash
    }

    override fun hashCode(): Int = hash?.hashCode() ?: 0
}
