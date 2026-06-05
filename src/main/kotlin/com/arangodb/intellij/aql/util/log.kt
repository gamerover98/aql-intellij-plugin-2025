package com.arangodb.intellij.aql.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger

object log {
    private val logger = Logger.getInstance(log::class.java.name)
    const val DISPLAY_ID = "AQL"

    @JvmStatic
    fun errorAction(message: String, actionName: String, callback: ActionMessageCallback) {
        val html = "$message<br/><a href=''>$actionName</a>"
        notify(Notification(DISPLAY_ID, DISPLAY_ID, html, NotificationType.ERROR) { _, _ -> callback.call() })
    }

    @JvmStatic
    fun info(message: String) {
        notify(Notification(DISPLAY_ID, DISPLAY_ID, message, NotificationType.INFORMATION))
    }

    @JvmStatic
    fun info(title: String, message: String) {
        val content = "$title<br /><strong>$message</strong>"
        notify(Notification(DISPLAY_ID, DISPLAY_ID, content, NotificationType.INFORMATION))
    }

    @JvmStatic
    fun error(message: String) {
        notify(Notification(DISPLAY_ID, DISPLAY_ID, message, NotificationType.ERROR))
    }

    @JvmStatic
    fun error(title: String, message: String) {
        notify(Notification(DISPLAY_ID, DISPLAY_ID, message, NotificationType.ERROR))
    }

    @JvmStatic
    fun warn(message: String) {
        notify(Notification(DISPLAY_ID, DISPLAY_ID, message, NotificationType.WARNING))
    }

    @JvmStatic
    fun warn(title: String, message: String) {
        notify(Notification(DISPLAY_ID, DISPLAY_ID, message, NotificationType.WARNING))
    }

    @JvmStatic
    fun debug(message: String) {
        logger.debug(message)
    }

    @JvmStatic
    fun debug(message: String, e: Throwable) {
        logger.debug(message, e)
    }

    private fun notify(notification: Notification) {
        Notifications.Bus.notify(notification)
    }
}
