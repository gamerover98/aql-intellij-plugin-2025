package com.arangodb.intellij.aql.actions

import com.intellij.openapi.actionSystem.AnActionEvent

class AqlExplainQueryAction : AqlQueryAction() {

    override fun actionPerformed(event: AnActionEvent) {
        runQueryAction(event, AqlDataService.QueryType.EXPLAIN_QUERY)
    }
}
