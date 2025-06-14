package com.arangodb.intellij.aql.fileTypes

import com.arangodb.intellij.aql.lang.AqlLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * Represents an AQL file in the IntelliJ Platform PSI.
 *
 * @param viewProvider The file view provider.
 */
open class AqlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AqlLanguage) {
    /** @return The file type associated with AQL files. */
    override fun getFileType(): FileType = AqlFileType // <-- Singleton

    /** @return A string representation of the AQL file. */
    override fun toString(): String = "AQL file: ${super.toString()}"
}