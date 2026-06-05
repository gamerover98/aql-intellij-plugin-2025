package com.arangodb.intellij.aql.ui.dialogs

import com.arangodb.intellij.aql.util.AqlUtils
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBDimension
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JPanel

class AqlParameterDialog(
    project: Project,
    names: Set<String>,
    element: PsiElement?
) : DialogWrapper(project) {

    companion object {
        private val PATTERN_AT = Pattern.compile("@")
    }

    @JvmField var panel: JPanel? = null
    @JvmField var tabbedPane: JBTabbedPane? = null
    @JvmField var contentPanel: JPanel? = null

    private val fields: MutableMap<String, JBTextField> = HashMap()

    init {
        tabbedPane?.removeAll()
        for (name in names) {
            val tabPanel = JPanel()
            val text = JBTextField(element?.let { AqlUtils.guessValueForParameter(name, it) } ?: "")
            text.preferredSize = JBDimension(200, 24)
            tabPanel.add(text)
            tabbedPane?.addTab(name, Icons.ICON_PARAMETER, tabPanel)
            fields[name] = text
        }
        Disposer.register(project, myDisposable)
        init()
    }

    override fun doValidate(): ValidationInfo? = null

    override fun createCenterPanel(): JComponent? = panel

    fun getData(): Map<String, String> =
        fields.entries.associate { (key, field) ->
            PATTERN_AT.matcher(key).replaceFirst("") to field.text
        }
}
