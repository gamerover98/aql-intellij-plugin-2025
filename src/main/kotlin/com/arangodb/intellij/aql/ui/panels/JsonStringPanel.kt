package com.arangodb.intellij.aql.ui.panels

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.Timer

/**
 * Displays the raw AQL query result as a compact, escaped JSON string —
 * suitable for pasting directly into Java/Kotlin/JavaScript code.
 *
 * Example output:
 *   "[{\"_id\":\"chars/1\",\"name\":\"Ned\"}]"
 */
class JsonStringPanel : JPanel(BorderLayout()) {

    private val mapper = ObjectMapper()
    private val textArea = JTextArea()
    private lateinit var copyBtn: JButton
    private var feedbackTimer: Timer? = null

    init {
        background = JBColor.background()

        textArea.isEditable = false
        textArea.lineWrap = true
        textArea.wrapStyleWord = false
        textArea.font = JBUI.Fonts.create(Font.MONOSPACED, JBUI.Fonts.label().size)
        textArea.background = JBColor.background()
        textArea.foreground = JBColor.foreground()
        textArea.border = JBUI.Borders.empty(6, 8)

        copyBtn = JButton("Copy", AllIcons.Actions.Copy).apply {
            toolTipText = "Copy the string to clipboard"
            addActionListener {
                copyToClipboard(textArea.text)
                showCopiedFeedback()
            }
        }

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
            add(copyBtn)
        }

        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(textArea), BorderLayout.CENTER)
    }

    fun setData(json: String) {
        if (json.isBlank()) { textArea.text = ""; return }
        val compact = try {
            mapper.writeValueAsString(mapper.readTree(json))
        } catch (_: Exception) { json }
        val escaped = compact.replace("\\", "\\\\").replace("\"", "\\\"")
        textArea.text = "\"$escaped\""
        textArea.caretPosition = 0
    }

    fun clear() { textArea.text = "" }

    // ─── Copy feedback ────────────────────────────────────────────────────────

    private fun showCopiedFeedback() {
        feedbackTimer?.stop()
        val origText = copyBtn.text
        val origIcon = copyBtn.icon
        copyBtn.text = "Copied!"
        copyBtn.icon = AllIcons.General.InspectionsOK
        copyBtn.foreground = JBColor(Color(0x2E7D32), Color(0x66BB6A))
        copyBtn.isEnabled = false
        feedbackTimer = Timer(1500) {
            copyBtn.text = origText
            copyBtn.icon = origIcon
            copyBtn.foreground = JBColor.foreground()
            copyBtn.isEnabled = true
        }.also { it.isRepeats = false; it.start() }
    }

    private fun copyToClipboard(text: String) =
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
