package com.arangodb.intellij.aql.services

import com.arangodb.intellij.aql.ArangoBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ArangoProjectService(project: Project) {

    init {
        thisLogger().info(ArangoBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}
