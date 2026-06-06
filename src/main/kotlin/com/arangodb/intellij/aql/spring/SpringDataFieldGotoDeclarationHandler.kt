package com.arangodb.intellij.aql.spring

import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

/**
 * Handles Ctrl+Click on AQL field references inside `@Query` annotations in
 * Spring Data ArangoDB repositories.
 *
 * When the cursor is on a property name in a path expression (e.g. `surname` in
 * `c.surname`), this handler resolves the corresponding field in the entity class
 * declared as the first type parameter of the repository interface, then navigates
 * the editor to that field declaration.
 *
 * Two resolution paths:
 *  - **Path A (AQL injection active)**: `sourceElement` is an AQL PSI element from the
 *    language injection created by [AqlQueryAnnotationInjector]. The handler checks that
 *    the element is inside a `PropertyLookup`, resolves the injection host back to the
 *    Java `PsiLiteralExpression`, and proceeds from there.
 *  - **Path B (Java-level fallback)**: `sourceElement` is a plain Java token inside a
 *    `PsiLiteralExpression`. The handler uses the `offset` to locate the word at the
 *    cursor and checks whether it is immediately preceded by `.` in the source text.
 *
 * Field resolution order (same for both paths):
 *  1. **Direct Java field name** (`Character#surname`)
 *  2. **`@Field("surname")` annotation** on a differently-named Java field
 *
 * Returns `null` (handler abstains) when:
 *  - the cursor is not on a property-access token
 *  - the AQL is not inside a `@Query` annotation
 *  - the entity class cannot be inferred from the repository type parameter
 *  - no matching field is found
 */
class SpringDataFieldGotoDeclarationHandler : GotoDeclarationHandler {

    companion object {
        private const val QUERY_ANNOTATION = "com.arangodb.springframework.annotation.Query"
        private const val FIELD_ANNOTATION  = "com.arangodb.springframework.annotation.Field"
    }

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val sourceEl = sourceElement ?: return null

        // ── Path A: AQL injection is active ───────────────────────────────────
        // AqlQueryAnnotationInjector produces AQL PSI elements inside @Query strings.
        // If that's what we have, use the fast, structured AQL path.
        aqlInjectedPath(sourceEl)?.let { return it }

        // ── Path B: Java-level fallback ───────────────────────────────────────
        // If injection hasn't fired yet, or is otherwise unavailable, fall back to
        // text analysis of the raw Java string literal at the cursor position.
        return javaLiteralPath(sourceEl, offset)
    }

    // ─── Path A: injection-based ──────────────────────────────────────────────

    private fun aqlInjectedPath(sourceEl: PsiElement): Array<PsiElement>? {
        // Normalize to the PropertyName (aqlType=ID) AQL element
        val idElement: AqlNamedElement = when {
            sourceEl is AqlNamedElement && sourceEl.aqlType == AqlMixinType.ID ->
                sourceEl
            sourceEl.parent is AqlNamedElement &&
                    (sourceEl.parent as AqlNamedElement).aqlType == AqlMixinType.ID ->
                sourceEl.parent as AqlNamedElement
            else -> return null
        }

        // Must be inside a PropertyLookup node (i.e. the RHS of a `.` operator).
        // Collection names and loop variables are NOT inside PropertyLookup.
        val lookupParent = idElement.parent
        if (lookupParent !is AqlNamedElement ||
            lookupParent.aqlType != AqlMixinType.PROPERTY_LOOKUP
        ) return null

        // Strip optional backtick quoting (`surname` → surname)
        val fieldName = idElement.name?.trim('`')?.takeIf { it.isNotBlank() } ?: return null

        // Require injection context: element must live inside a Java string literal
        val injectionHost = InjectedLanguageManager.getInstance(sourceEl.project)
            .getInjectionHost(sourceEl) ?: return null

        return resolveFromHost(injectionHost, fieldName)
    }

    // ─── Path B: Java literal text analysis ───────────────────────────────────

    private fun javaLiteralPath(sourceEl: PsiElement, offset: Int): Array<PsiElement>? {
        // Find the PsiLiteralExpression that contains the cursor
        val literal: PsiLiteralExpression =
            when {
                sourceEl is PsiLiteralExpression -> sourceEl
                sourceEl.parent is PsiLiteralExpression ->
                    sourceEl.parent as PsiLiteralExpression
                else -> return null
            }

        // Must be a string literal
        if (literal.value !is String) return null

        val literalText = literal.text   // includes surrounding quotes, e.g. "FOR c IN..."
        val contentStart = literal.textRange.startOffset + 1  // offset of char after opening "

        val posInContent = offset - contentStart
        if (posInContent < 0 || posInContent >= literalText.length - 1) return null

        // Extract the identifier word at posInContent (using the raw literal text
        // without the surrounding quotes, so index 0 = first char of the string content)
        val content = literalText.substring(1, literalText.length - 1)
        val fieldName = propertyNameAt(content, posInContent) ?: return null

        return resolveFromHost(literal, fieldName)
    }

    /**
     * Returns the identifier word at [pos] in [content] if and only if that word is
     * immediately preceded by a `.` character (AQL property-access pattern `var.field`).
     * Returns `null` if the cursor is not on an identifier, or if the identifier is not
     * preceded by `.`.
     */
    private fun propertyNameAt(content: String, pos: Int): String? {
        if (pos < 0 || pos >= content.length) return null
        val c = content[pos]
        if (!c.isLetterOrDigit() && c != '_') return null  // cursor is between tokens

        // Walk left to find word start
        var start = pos
        while (start > 0 && (content[start - 1].isLetterOrDigit() || content[start - 1] == '_')) {
            start--
        }

        // Must be preceded by '.' to qualify as a property access
        if (start == 0 || content[start - 1] != '.') return null

        // Walk right to find word end
        var end = pos
        while (end < content.length - 1 &&
               (content[end + 1].isLetterOrDigit() || content[end + 1] == '_')
        ) {
            end++
        }

        return content.substring(start, end + 1).takeIf { it.isNotBlank() }
    }

    // ─── Shared resolution (both paths converge here) ────────────────────────

    /**
     * Given a [host] `PsiLiteralExpression` (the Java string in the `@Query` annotation)
     * and a resolved [fieldName], navigates to the matching field in the entity class.
     */
    private fun resolveFromHost(host: PsiElement, fieldName: String): Array<PsiElement>? {
        val annotation = PsiTreeUtil.getParentOfType(host, PsiAnnotation::class.java)
            ?: return null
        if (annotation.qualifiedName != QUERY_ANNOTATION) return null

        val method = PsiTreeUtil.getParentOfType(annotation, PsiMethod::class.java)
            ?: return null
        val repository = method.containingClass ?: return null
        val entityClass = resolveEntityClass(repository) ?: return null

        return findEntityField(entityClass, fieldName)
    }

    // ─── Entity-class resolution ──────────────────────────────────────────────

    /**
     * Returns the entity class (first type parameter) from a Spring Data repository.
     *
     * For `CharacterRepository extends ArangoRepository<Character, String>`,
     * this resolves the `Character` class.
     *
     * Searches all direct `superTypes` of [cls] and returns the first one whose
     * first type argument resolves to a concrete `PsiClass`.
     */
    private fun resolveEntityClass(cls: PsiClass): PsiClass? {
        for (superType in cls.superTypes) {
            val typeArgs = superType.parameters
            if (typeArgs.isNotEmpty()) {
                val resolved = (typeArgs[0] as? PsiClassType)?.resolve()
                if (resolved != null) return resolved
            }
        }
        return null
    }

    /**
     * Finds the PSI field corresponding to [fieldName] in [entityClass] (and its
     * superclass hierarchy via `findFieldByName`).
     *
     * Tries:
     *  1. Direct Java field name match (`Character#surname`)
     *  2. `@Field("surname")` annotation on a differently-named field
     */
    private fun findEntityField(entityClass: PsiClass, fieldName: String): Array<PsiElement>? {
        // 1. Direct field name
        entityClass.findFieldByName(fieldName, true)?.let { return arrayOf(it) }

        // 2. @Field annotation value match
        val annotatedField = entityClass.allFields.firstOrNull { field ->
            field.getAnnotation(FIELD_ANNOTATION)
                ?.findDeclaredAttributeValue("value")
                ?.let { (it as? PsiLiteralExpression)?.value as? String } == fieldName
        }
        return if (annotatedField != null) arrayOf(annotatedField) else null
    }
}
