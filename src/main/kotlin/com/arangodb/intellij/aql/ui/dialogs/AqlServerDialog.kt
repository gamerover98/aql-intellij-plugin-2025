package com.arangodb.intellij.aql.ui.dialogs

import com.arangodb.intellij.aql.actions.ActionResponse
import com.arangodb.intellij.aql.actions.AqlDataService
import com.arangodb.intellij.aql.model.ArangoDbServer
import com.arangodb.intellij.aql.util.log
import com.intellij.ide.ui.UINumericRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import java.awt.Dimension
import java.awt.Point
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class AqlServerDialog(private val project: Project) : DialogWrapper(project) {

    @JvmField var panel: JPanel? = null
    @JvmField var serverNameText: JBTextField? = null
    @JvmField var hostText: JBTextField? = null
    @JvmField var userText: JBTextField? = null
    @JvmField var passwordText: JBPasswordField? = null
    @JvmField var testServerButton: JButton? = null
    @JvmField var portSpinner: JBIntSpinner? = null
    @JvmField var tabbedPane: JBTabbedPane? = null
    @JvmField var excludeSystemCheckbox: JBCheckBox? = null
    @JvmField var useSslCheckbox: JBCheckBox? = null

    init {
        Disposer.register(project, myDisposable)
        init()
        testServerButton?.addActionListener {
            val result = AqlDataService.with(project).testServerConnection(buildState())
            showTooltip(result)
            if (result.type == ActionResponse.Type.ERROR) log.error("Server Connection:", result.message)
            else log.info("Server Connection:", result.message)
        }
    }

    private fun createUIComponents() {
        portSpinner = JBIntSpinner(UINumericRange(8529, 0, 65535))
    }

    fun showTooltip(response: ActionResponse) {
        ApplicationManager.getApplication().invokeLater {
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(response.message, response.getIcon(), MessageType.INFO.popupBackground, null)
                .setFadeoutTime(3000)
                .setShowCallout(false)
                .createBalloon()
            val size: Dimension = panel?.size ?: return@invokeLater
            val point = RelativePoint(panel!!, Point(size.width / 2, size.height / 2))
            balloon.show(point, Balloon.Position.above)
        }
    }

    override fun doValidate(): ValidationInfo? = null

    override fun createCenterPanel(): JComponent? = panel

    private fun buildState(): ArangoDbServer = ArangoDbServer().also { state ->
        state.name = serverNameText?.text
        state.user = userText?.text
        state.password = String(passwordText?.password ?: CharArray(0))
        state.host = hostText?.text ?: "127.0.0.1"
        state.port = portSpinner?.number ?: ArangoDbServer.DEFAULT_PORT
        state.isExcludeSystemCollections = excludeSystemCheckbox?.isSelected ?: true
        state.isUseSsl = useSslCheckbox?.isSelected ?: false
    }

    fun getData(): ArangoDbServer = buildState()

    fun setData(server: ArangoDbServer) {
        serverNameText?.text = server.name
        passwordText?.text = server.password
        userText?.text = server.user
        hostText?.text = server.host
        portSpinner?.number = server.port
        excludeSystemCheckbox?.isSelected = server.isExcludeSystemCollections
        useSslCheckbox?.isSelected = server.isUseSsl
    }
}
