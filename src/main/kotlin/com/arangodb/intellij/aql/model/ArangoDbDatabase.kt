package com.arangodb.intellij.aql.model

import com.arangodb.entity.CollectionEntity
import com.arangodb.entity.GraphEntity
import com.arangodb.entity.ViewEntity

class ArangoDbDatabase {
    var name: String? = null
    var collections: Collection<CollectionEntity>? = null
    var graphs: Collection<GraphEntity>? = null
    var views: Collection<ViewEntity>? = null

    constructor()

    constructor(name: String) {
        this.name = name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArangoDbDatabase) return false
        return name == other.name &&
                collections == other.collections &&
                graphs == other.graphs &&
                views == other.views
    }

    override fun hashCode(): Int = arrayOf(name, collections, graphs, views).contentHashCode()
}
