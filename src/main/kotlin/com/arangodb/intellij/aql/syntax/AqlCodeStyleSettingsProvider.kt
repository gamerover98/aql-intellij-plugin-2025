package com.arangodb.intellij.aql.syntax

import com.arangodb.intellij.aql.lang.AqlLanguage
import com.arangodb.intellij.aql.util.AQL_LANGUAGE_ID
import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

/**
 * Provides code style settings integration for the AQL language in IntelliJ IDEA.
 * This class allows users to configure and customize code formatting options for AQL files
 * via the IDE's code style settings UI.
 */
class AqlCodeStyleSettingsProvider : CodeStyleSettingsProvider() {

    /**
     * Creates custom code style settings specific to AQL.
     *
     * @param settings The global code style settings.
     * @return An instance of [AqlCodeStyleSettings].
     */
    override fun createCustomSettings(settings: CodeStyleSettings) = AqlCodeStyleSettings(settings)

    /**
     * Returns the display name for the AQL code style configurable.
     *
     * @return The display name as defined in [AQL_LANGUAGE_ID].
     */
    override fun getConfigurableDisplayName() = AQL_LANGUAGE_ID

    /**
     * Creates the code style configurable for the AQL language.
     *
     * @param settings The current code style settings.
     * @param originalSettings The original code style settings.
     * @return A [CodeStyleConfigurable] for AQL.
     */
    override fun createConfigurable(
        settings: CodeStyleSettings,
        originalSettings: CodeStyleSettings
    ): CodeStyleConfigurable =
        object : CodeStyleAbstractConfigurable(
            settings,
            originalSettings,
            AQL_LANGUAGE_ID
        ) {
            /**
             * Creates the main panel for configuring AQL code style settings.
             *
             * @param settings The code style settings to be edited.
             * @return The main code style panel for AQL.
             */
            override fun createPanel(settings: CodeStyleSettings) =
                SimpleCodeStyleMainPanel(currentSettings, settings)

            /**
             * Returns the help topic for the AQL code style settings.
             *
             * @return Always `null` as no help topic is provided.
             */
            override fun getHelpTopic(): String? = null
        }

    /**
     * Main panel for AQL code style settings, displayed in the code style configuration UI.
     *
     * @param currentSettings The current code style settings.
     * @param settings The code style settings to be edited.
     */
    private class SimpleCodeStyleMainPanel(
        currentSettings: CodeStyleSettings?,
        settings: CodeStyleSettings
    ) : TabbedLanguageCodeStylePanel(AqlLanguage, currentSettings, settings)
}