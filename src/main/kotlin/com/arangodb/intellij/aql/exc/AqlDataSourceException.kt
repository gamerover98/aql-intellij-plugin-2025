package com.arangodb.intellij.aql.exc

class AqlDataSourceException : Throwable {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
