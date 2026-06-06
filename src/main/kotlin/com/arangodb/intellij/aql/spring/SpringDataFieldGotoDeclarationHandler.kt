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
 * Resolution order:
 *  1. **Direct name** — a Java field whose simple name equals the AQL property name.
 *  2. **`@Field` annotation** — a field annotated with `@Field("surname")` even if its
 *     Java name differs (common when the DB field name and the Java field name diverge).
 *
 * The handler is a no-op (returns `null`) when:
 *  - the element is not a property lookup (e.g. a plain collection name or variable)
 *  - the AQL is not inside an injected Java string (i.e. a standalone `.aql` file)
 *  - the containing annotation is not `@Query`
 *  - the entity class cannot be inferred from the repository type parameter
 *  - no matching field is found in the entity class hierarchy
 */
class SpringDataFieldGotoDeclarationHandler : GotoDeclarationHandler {

    companion object {
        private const val QUERY_ANNOTATION = "com.arangodb.springframework.annotation.Query"
        private const val FIELD_ANNOTATION = "com.arangodb.springframework.annotation.Field"
    }

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val sourceEl = sourceElement ?: return null

        // ── 1. Normalize to the PropertyName (aqlType=ID) element ─────────────
        //    The leaf under the cursor is either the AqlNamedElement itself or its child.
        val idElement: AqlNamedElement = when {
            sourceEl is AqlNamedElement && sourceEl.aqlType == AqlMixinType.ID ->
                sourceEl
            sourceEl.parent is AqlNamedElement &&
                    (sourceEl.parent as AqlNamedElement).aqlType == AqlMixinType.ID ->
                sourceEl.parent as AqlNamedElement
            else -> return null
        }

        // ── 2. Must be inside a PropertyLookup (not a stand-alone collection/var ref) ─
        //    In `c.surname`, `surname` is a PropertyName child of PropertyLookup.
        //    Collection names and loop variables are not inside PropertyLookup.
        val lookupParent = idElement.parent
        if (lookupParent !is AqlNamedElement ||
            lookupParent.aqlType != AqlMixinType.PROPERTY_LOOKUP
        ) return null

        // Strip backtick quoting (e.g. `surname` → surname)
        val fieldName = idElement.name?.trim('`')?.takeIf { it.isNotBlank() } ?: return null

        // ── 3. AQL must be language-injected into a Java string literal ────────
        //    Returns null for plain .aql files where no injection host exists.
        val injectionHost = InjectedLanguageManager.getInstance(sourceEl.project)
            .getInjectionHost(sourceEl) ?: return null

        // ── 4. Injection host must live inside a @Query annotation ─────────────
        val annotation = PsiTreeUtil.getParentOfType(injectionHost, PsiAnnotation::class.java)
            ?: return null
        if (annotation.qualifiedName != QUERY_ANNOTATION) return null

        // ── 5. Find the containing repository method and its interface ─────────
        val method = PsiTreeUtil.getParentOfType(annotation, PsiMethod::class.java) ?: return null
        val repository = method.containingClass ?: return null

        // ── 6. Resolve the entity class from the repository's first type argument ─
        val entityClass = resolveEntityClass(repository) ?: return null

        // ── 7. Navigate to the matching field in the entity class hierarchy ─────
        return findEntityField(entityClass, fieldName)
    }

    // ─── Resolution helpers ───────────────────────────────────────────────────

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
     *  2. `@Field("surname")` annotation on any field in the hierarchy
     */
    private fun findEntityField(entityClass: PsiClass, fieldName: String): Array<PsiElement>? {
        // 1. Direct field name
        entityClass.findFieldByName(fieldName, true)?.let { return arrayOf(it) }

        // 2. @Field annotation value match — handles DB/Java name divergence,
        //    e.g. @Field("surname") on a field declared as `familyName`
        val annotatedField = entityClass.allFields.firstOrNull { field ->
            field.getAnnotation(FIELD_ANNOTATION)
                ?.findDeclaredAttributeValue("value")
                ?.let { (it as? PsiLiteralExpression)?.value as? String } == fieldName
        }
        return if (annotatedField != null) arrayOf(annotatedField) else null
    }
}
