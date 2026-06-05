package com.arangodb.intellij.aql.ui.actions

import com.arangodb.intellij.aql.actions.ActionBusEvent
import com.arangodb.intellij.aql.actions.ActionEventData
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnActionButton

class CleanConsoleAction : AnActionButton("Clear Console", "", Icons.ICON_DELETE) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = getEventProject(e) ?: return
        val event = project.messageBus.syncPublisher(ActionBusEvent.AQL_SYSTEM_EMPTY_LOG)
        event.onEvent(ActionEventData.EMPTY)
    }
}
