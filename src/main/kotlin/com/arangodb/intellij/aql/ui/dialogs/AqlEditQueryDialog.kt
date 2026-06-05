package com.arangodb.intellij.aql.ui.dialogs

import com.arangodb.intellij.aql.model.AqlQuery
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBDimension
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout

class AqlEditQueryDialog(project: Project, private val query: AqlQuery) : DialogWrapper(project) {

    private val textArea = JBTextArea(query.query ?: "").apply {
        lineWrap = true
        wrapStyleWord = true
        preferredSize = JBDimension(600, 300)
    }

    init {
        title = "Edit Query: ${query.name}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(JBScrollPane(textArea), BorderLayout.CENTER)
        return panel
    }

    fun getUpdatedQuery(): AqlQuery {
        val updated = AqlQuery(query.name ?: "", textArea.text, query.getParameters().toMutableMap())
        updated.hash = query.hash
        return updated
    }
}
