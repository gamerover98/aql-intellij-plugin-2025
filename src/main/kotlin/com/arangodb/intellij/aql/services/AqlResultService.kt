package com.arangodb.intellij.aql.services

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class AqlResultService {
    var lastResult: String = ""
}
