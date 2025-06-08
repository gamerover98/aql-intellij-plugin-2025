package com.arangodb.intellij.aql

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * Provides access to localized messages for the ArangoDB plugin.
 *
 * Uses `DynamicBundle` to retrieve strings from the resource file
 * `messages/ArangoBundle.properties`. Offers static methods to get
 * formatted messages or lazy message pointers.
 */
@NonNls
private const val BUNDLE = "messages.ArangoBundle"

/**
 * Singleton for accessing localized messages of the ArangoDB plugin.
 */
object ArangoBundle : DynamicBundle(BUNDLE) {

    /**
     * Returns a localized message formatted with the provided parameters.
     *
     * @param key Message key in the resource file.
     * @param params Optional parameters for message formatting.
     * @return Formatted localized message.
     */
    @JvmStatic
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ) = getMessage(key, *params)

    /**
     * Returns a lazy pointer to a localized message.
     *
     * @param key Message key in the resource file.
     * @param params Optional parameters for message formatting.
     * @return Function that returns the localized message when invoked.
     */
    @JvmStatic
    @Suppress("unused")
    fun messagePointer(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ) = getLazyMessage(key, *params)
}