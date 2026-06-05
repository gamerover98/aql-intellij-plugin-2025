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
}
