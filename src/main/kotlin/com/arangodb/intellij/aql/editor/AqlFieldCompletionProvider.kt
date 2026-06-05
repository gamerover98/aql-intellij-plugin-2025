package com.arangodb.intellij.aql.editor

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.util.Icons
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Provides field name completions for dot-notation property access.
 *
 * When the user types `doc.fieldName`, this provider:
 * 1. Detects the variable name being accessed (the part before the dot)
 * 2. Finds the FOR binding for that variable in the current file
 * 3. Fetches sampled field names from the bound collection
 * 4. Returns them as completion suggestions
 *
 * Binding detection uses a regex over the file text — fast and sufficient for common cases.
 */
class AqlFieldCompletionProvider : CompletionProvider<CompletionParameters>() {

    // Matches: FOR varName IN collectionName (ignoring extra whitespace)
    private val FOR_BINDING_REGEX = Regex(
        """(?i)\bFOR\s+(\w+)\s+IN\s+`?(\w+)`?""",
        RegexOption.MULTILINE
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val project = position.project

        // Identify the variable being accessed before the dot.
        // In `doc.field`, position is the field element; its parent is AqlPropertyLookup
        // which holds the expression before the dot.
        val parent = position.parent ?: return
        val grandParent = parent.parent ?: return

        // The expression before the dot: typically a leaf token with the variable name
        val expressionText = grandParent.firstChild?.text?.trim() ?: return
        if (expressionText.isBlank() || expressionText == parent.text) return

        // Find the collection bound to this variable via FOR varName IN collectionName
        val fileText = position.containingFile?.text ?: return
        val collectionName = FOR_BINDING_REGEX.findAll(fileText)
            .firstOrNull { it.groupValues[1] == expressionText }
            ?.groupValues?.getOrNull(2) ?: return

        val service = project.getService(AqlDatabaseService::class.java)
        val fields = service.getFieldNames(collectionName, project)
        if (fields.isEmpty()) return

        result.addAllElements(
            fields.map { field ->
                LookupElementBuilder.create(field)
                    .withIcon(Icons.ICON_PROPERTY)
                    .withTypeText(collectionName)
                    .withCaseSensitivity(true)
            }
        )
    }
}
