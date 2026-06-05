package com.arangodb.intellij.aql.ui.renderers

import com.arangodb.intellij.aql.util.Icons
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

class AqlQueryModel(
    var name: String? = null,
    parameters: Map<String, String>? = null,
    var type: Type = Type.TEXT_ONLY
) {
    enum class Type { ROOT, WITH_PARAMS, TEXT_ONLY, EXPLAIN, PROFILE }

    private var _parameters: Map<String, String>? = parameters
    var isSelected: Boolean = false

    constructor(name: String, type: Type) : this(name, null, type)

    fun getParameters(): Map<String, String> = _parameters ?: emptyMap()
    fun setParameters(parameters: Map<String, String>) { _parameters = parameters }

    fun getIcon(): Icon = when (type) {
        Type.TEXT_ONLY -> Icons.ICON_TEXT
        Type.WITH_PARAMS -> Icons.ICON_PARAMETER
        Type.ROOT -> Icons.ICON_QUERY
        else -> Icons.ICON_TEXT
    }

    fun getStyle(): SimpleTextAttributes {
        if (isSelected) return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
        return when (type) {
            Type.EXPLAIN, Type.PROFILE -> SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
            else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
        }
    }

    override fun toString(): String = name ?: ""
}
