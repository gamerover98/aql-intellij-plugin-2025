package com.arangodb.intellij.aql.syntax

import com.arangodb.intellij.aql.lang.AqlLanguage
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

/**
 * Provides code style settings for the AQL language in IntelliJ IDEA.
 *
 * This class integrates AQL-specific code style options into the IDE's
 * code style settings UI, allowing users to view and edit formatting
 * preferences for AQL files.
 */
class AqlLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    /** Returns the AQL language instance for which this provider supplies code style settings. */
    override fun getLanguage() = AqlLanguage

    /**
     * Returns a sample AQL code snippet to be displayed in the code style settings preview panel.
     *
     * @param settingsType The type of code style settings being shown.
     * @return A sample AQL code as a string.
     */
    override fun getCodeSample(
        settingsType: SettingsType
    ) = """// example
           /*  code sample*/
           FOR doc IN collection
           RETURN doc
           LIMIT 1,2
           SORT doc._from ASC
           RETURN {document:doc}
           """.trimIndent()
}