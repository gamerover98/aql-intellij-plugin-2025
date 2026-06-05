package com.arangodb.intellij.aql.ui

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.Logger

object PasswordManager {

    private val logger = Logger.getInstance(PasswordManager::class.java)

    @JvmStatic
    fun save(key: String, value: String) {
        try {
            PasswordSafe.instance.set(
                CredentialAttributes(PasswordManager::class.java.name, key),
                Credentials(key, value)
            )
        } catch (e: Exception) {
            logger.error("Cannot store password", e)
        }
    }

    @JvmStatic
    fun load(key: String): String? {
        return try {
            PasswordSafe.instance.get(
                CredentialAttributes(PasswordManager::class.java.name, key)
            )?.getPasswordAsString()
        } catch (e: Exception) {
            logger.error("Cannot load password", e)
            null
        }
    }
}
