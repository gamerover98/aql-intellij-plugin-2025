package com.arangodb.intellij.aql.util

// TODO: fixme: uncomment and fix the test class - extend BasePlatformTestCase
class AqlUtilsTest {

    // @Test // TODO: fixme: uncomment and fix the test
    fun testParameterNames() {
        val parameterName = "@someName"
        val parameterName2 = "@myLimit"
        val parameterName3 = "@@collection"
        @Suppress("UNUSED_VARIABLE")
        val query = "FOR doc in $parameterName3 FILTER doc.name = $parameterName LIMIT $parameterName2 RETURN doc"
        // val parameters = AqlUtils.extractParameterNames(query, getProject())
        // assertEquals(3, parameters.size)
        // assertTrue(parameters.contains(parameterName))
    }
}
