package com.arangodb.intellij.aql.ui.console

import com.arangodb.intellij.aql.fileTypes.AqlFileType
import com.intellij.testFramework.LightVirtualFile

/**
 * In-memory virtual file that represents a single query-result tab.
 *
 * Unlike [AqlConsoleVirtualFile], this is NOT a singleton: a new instance is created
 * for every query execution so that multiple result tabs can coexist simultaneously,
 * enabling side-by-side comparison of different query results.
 *
 * [queryId] is a UUID that ties this file to one specific query execution. The
 * corresponding [AqlResultFileEditor] uses it to filter bus events so only the
 * right result populates the right tab.
 */
class AqlResultVirtualFile(title: String, val queryId: String)
    : LightVirtualFile("$title.aql", AqlFileType, "") {

    override fun isWritable(): Boolean = false
}
