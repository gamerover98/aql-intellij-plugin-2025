package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.util.AQL_LANGUAGE_ID
import com.intellij.lang.Language
import java.io.Serializable

/**
 * Singleton representing the AQL language for IntelliJ support.
 *
 * Implements `Serializable` to ensure that deserialization preserves the singleton property.
 * The `readResolve()` method guarantees that deserialization always returns the singleton instance,
 * preventing the creation of a new instance.
 */
object AqlLanguage : Language(AQL_LANGUAGE_ID), Serializable {

    /** Ensures that deserialization returns the singleton instance. */
    @JvmStatic
    @Suppress("unused")
    private fun readResolve(): Any = AqlLanguage
}