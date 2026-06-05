package com.arangodb.intellij.aql.actions

import com.arangodb.intellij.aql.util.Icons
import javax.swing.Icon

class ActionResponse private constructor(val message: String, val type: Type = Type.INFO) {

    enum class Type { INFO, ERROR }

    fun isError(): Boolean = type == Type.ERROR

    fun getIcon(): Icon = if (isError()) Icons.ICON_ERROR else Icons.ICON_ARANGO

    companion object {
        @JvmStatic fun error(message: String): ActionResponse = ActionResponse(message, Type.ERROR)
        @JvmStatic fun info(message: String): ActionResponse = ActionResponse(message)
    }
}
