/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arangodb.intellij.aql.intentions

import com.intellij.AbstractBundle
import com.intellij.reference.SoftReference
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.util.*

/**
 * Utility object for accessing localized messages from the AQL language resource bundle.
 * Provides methods to retrieve internationalized strings for use in the plugin UI and logic.
 */
object AqlLanguageBundle {

    private const val BUNDLE: String = "AqlLanguageBundle"

    /**
     * Returns a localized message for the given key and optional parameters.
     *
     * @param key the key for the desired string in the resource bundle
     * @param params optional parameters to format the message
     * @return the localized message string
     */
    @JvmStatic
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): String = AbstractBundle.message(bundle, key, *params)

    /**
     * Soft reference to the loaded resource bundle to optimize memory usage.
     */
    @JvmStatic
    private var ourBundle: Reference<ResourceBundle>? = null

    /**
     * Lazily loads and caches the resource bundle for AQL language messages.
     */
    private val bundle: ResourceBundle
        get() =
            SoftReference.dereference(ourBundle)
                ?: ResourceBundle.getBundle(BUNDLE).also {
                    ourBundle = java.lang.ref.SoftReference(it)
                }
}