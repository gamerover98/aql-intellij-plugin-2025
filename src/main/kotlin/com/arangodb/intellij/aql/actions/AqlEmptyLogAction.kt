package com.arangodb.intellij.aql.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AqlEmptyLogAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = getEventProject(event) ?: return
        val busEvent = project.messageBus.syncPublisher(ActionBusEvent.AQL_SYSTEM_EMPTY_LOG)
        busEvent.onEvent(ActionEventData.EMPTY)
    }
}
