package com.arangodb.intellij.aql.util

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object Icons {
    @JvmField val ICON_PROFILE: Icon = get("/icons/profile.svg")
    @JvmField val ICON_RUN: Icon = get("/icons/run.svg")
    @JvmField val ICON_DELETE: Icon = get("/icons/trash.svg")
    @JvmField val ICON_SELECTED: Icon = get("/icons/selected.png")
    @JvmField val ICON_SYSTEM_ATTRIBUTE: Icon = get("/icons/system_attribute.svg")
    @JvmField val ICON_QUERY: Icon = get("/icons/query.svg")
    @JvmField val ICON_ERROR: Icon = get("/icons/error.png")
    @JvmField val ICON_ARANGO: Icon = get("/icons/aql.png")
    @JvmField val ICON_ARANGO_SMALL: Icon = get("/icons/aql-small.png")
    @JvmField val ICON_VIEW: Icon = get("/icons/view.png")
    @JvmField val ICON_TEXT: Icon = get("/icons/text.svg")
    @JvmField val ICON_COLLECTION: Icon = get("/icons/collection.png")
    @JvmField val ICON_EDGE: Icon = get("/icons/edge.png")
    @JvmField val ICON_GRAPH: Icon = get("/icons/graph.png")
    @JvmField val ICON_ID: Icon = get("/icons/id.png")
    @JvmField val ICON_PARAMETER: Icon = get("/icons/parameter.png")
    @JvmField val ICON_PROPERTY: Icon = get("/icons/property.png")
    @JvmField val ICON_PLACEHOLDER: Icon = get("/icons/placeholder.png")
    @JvmField val ICON_DATABASE: Icon = get("/icons/database.png")
    @JvmField val ICON_SERVER: Icon = get("/icons/server.png")
    @JvmField val ICON_EDIT: Icon = get("/icons/edit.svg")
    @JvmField val ICON_FUNCTION: Icon = get("/icons/function.png")

    private fun get(path: String): Icon = IconLoader.getIcon(path, Icons::class.java)
}
