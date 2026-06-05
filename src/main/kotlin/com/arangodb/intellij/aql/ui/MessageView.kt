package com.arangodb.intellij.aql.ui

import com.arangodb.intellij.aql.actions.ActionEventData
import com.intellij.openapi.project.Project

interface MessageView {
    fun onMessage(data: ActionEventData, project: Project)
    fun onClean(project: Project)
}
