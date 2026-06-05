package com.arangodb.intellij.aql.db

import com.arangodb.ArangoDatabase
import com.arangodb.intellij.aql.exc.AqlDataSourceException
import com.arangodb.intellij.aql.model.ArangoDbServer
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project

interface AqlDatabaseService {

    fun getSearchViews(): Collection<LookupElement>
    fun getCollections(): Collection<LookupElement>
    fun getGraphs(): Collection<LookupElement>
    fun getAll(): Collection<LookupElement>
    fun refresh(databaseSettings: ArangoDbServer, project: Project)

    @Throws(AqlDataSourceException::class)
    fun checkServerConnection(settings: ArangoDbServer, project: Project): ArangoDatabase

    @Throws(AqlDataSourceException::class)
    fun getDatabase(settings: ArangoDbServer): ArangoDatabase

    @Throws(AqlDataSourceException::class)
    fun getActiveDatabase(settings: ArangoDbServer, project: Project): ArangoDatabase

    @Throws(AqlDataSourceException::class)
    fun getServer(project: Project): ArangoDbServer
    fun isConnectionValid(project: Project): Boolean

    /** Returns sampled field names for the given collection. Empty list when not connected or unknown collection. */
    fun getFieldNames(collectionName: String, project: Project): List<String>

    /** Returns the document count for a collection, or -1 if unavailable. */
    fun getCollectionCount(collectionName: String, project: Project): Long
}
