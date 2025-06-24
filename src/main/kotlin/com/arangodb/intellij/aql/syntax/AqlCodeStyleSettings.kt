package com.arangodb.intellij.aql.syntax

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

/**
 * Custom code style settings for AQL language support in IntelliJ IDEA.
 * This class allows the plugin to define and persist user-specific formatting options
 * for AQL files, integrating with the IDE's code style framework.
 *
 * @param settings The global code style settings to which these custom settings are attached.
 */
class AqlCodeStyleSettings(settings: CodeStyleSettings) :
    CustomCodeStyleSettings("AqlCodeStyleSettings", settings)