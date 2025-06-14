package com.arangodb.intellij.aql.lang

import com.arangodb.intellij.aql.util.AqlUtils
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.util.containers.toArray

private val EMPTY_ARRAY = arrayOfNulls<String>(0)
private val EMPTY_NAV_ITEMS = arrayOfNulls<NavigationItem>(0)

/**
 * Provides support for the `Go to Symbol` and `Go to File`
 * features in IntelliJ for AQL language elements.
 *
 * This contributor allows users to quickly search and navigate
 * to named AQL elements within the project.
 */
class AqlChooseByNameContributor : ChooseByNameContributor {

    /** Returns an array of all named AQL elements in the project. */
    override fun getNames(
        project: Project?,
        includeNonProjectItems: Boolean
    ) = AqlUtils
        .findNamedElements(project)
        .mapNotNull { it.name?.takeIf { name -> name.isNotEmpty() } }
        .toArray(EMPTY_ARRAY)

    /** Returns an array of [NavigationItem] elements matching the given name in the project. */
    override fun getItemsByName(
        name: String?,
        pattern: String?,
        project: Project?,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem?> {
        //TODO: Include non project items.
        val properties = AqlUtils.findNamedElements(project, name)
        return properties.toArray(EMPTY_NAV_ITEMS)
    }
}