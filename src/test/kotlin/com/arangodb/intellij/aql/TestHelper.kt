package com.arangodb.intellij.aql

import com.arangodb.intellij.aql.util.AQL_FILE_EXTENSION
import java.io.File

object TestHelper {

    val EXTENSION = ".$AQL_FILE_EXTENSION"

    fun getTestDataPath(): String {
        val resource = TestHelper::class.java.classLoader.getResource("test.txt")!!
        return File(resource.file).parentFile.absolutePath + File.separatorChar + "testData" + File.separatorChar
    }
}
