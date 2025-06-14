package com.arangodb.intellij.aql.fileTypes

import com.arangodb.intellij.aql.lang.AqlLanguage
import com.arangodb.intellij.aql.util.AQL_FILE_DESCRIPTION
import com.arangodb.intellij.aql.util.AQL_FILE_EXTENSION
import com.arangodb.intellij.aql.util.AQL_FILE_NAME
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Singleton representing the AQL file type for the IntelliJ Platform.
 * Provides metadata such as name, description, default extension, and icon
 * for AQL files used in the IDE.
 */
object AqlFileType : LanguageFileType(AqlLanguage) {
    override fun getName(): String = AQL_FILE_NAME
    override fun getDescription(): String = AQL_FILE_DESCRIPTION
    override fun getDefaultExtension(): String = AQL_FILE_EXTENSION
    override fun getIcon(): Icon? = Icons.ICON_ARANGO_SMALL
}