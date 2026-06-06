package com.arangodb.intellij.aql.spring

import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiTreeUtil

/**
 * Handles Ctrl+Click inside `@Query` annotation strings in Spring Data ArangoDB repositories.
 *
 * Two navigation targets are supported:
 *
 * **Property field** (`c.surname` → `surname`):
 *  The cursor is on the RHS of a `.` operator (inside a `PropertyLookup` in the AQL PSI).
 *  Resolves the word as a Java field in the repository's entity class (first type parameter).
 *  Field lookup order:
 *   1. Direct Java field name (`Character#surname`)
 *   2. `@Field("surname")` annotation on a differently-named field
 *
 * **Collection name** (`FOR c IN characters` → `characters`):
 *  The cursor is on a plain identifier that is NOT a property access.
 *  Searches the project for a `@Document`-annotated class whose collection name matches.
 *  Convention: `@Document("characters")` explicit value, or lower-cased class name default.
 *
 * Two resolution paths (tried in order):
 *  - **Path A (AQL injection active)**: `sourceElement` is an AQL PSI element produced by
 *    [AqlQueryAnnotationInjector]. The PSI parent tells us whether we have a property lookup
 *    or a plain identifier.
 *  - **Path B (Java-level fallback)**: `sourceElement` is a Java token inside the string
 *    literal. Text analysis with the `offset` parameter determines whether the word is
 *    preceded by `.`.
 *
 * Returns `null` (abstains) when:
 *  - the cursor is not on an AQL identifier in a `@Query` annotation
 *  - the entity class or collection class cannot be resolved
 *  - no matching field/class is found
 */
class SpringDataFieldGotoDeclarationHandler : GotoDeclarationHandler {

    companion object {
        private const val QUERY_ANNOTATION    = "com.arangodb.springframework.annotation.Query"
        private const val DOCUMENT_ANNOTATION = "com.arangodb.springframework.annotation.Document"
        private const val FIELD_ANNOTATION    = "com.arangodb.springframework.annotation.Field"
    }

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val sourceEl = sourceElement ?: return null

        // Path A: AQL injection is active — sourceElement is an AQL PSI element
        aqlInjectedPath(sourceEl)?.let { return it }

        // Path B: Java-level fallback — sourceElement is a raw Java token
        return javaLiteralPath(sourceEl, offset)
    }

    // ─── Path A: injection-based ──────────────────────────────────────────────

    private fun aqlInjectedPath(sourceEl: PsiElement): Array<PsiElement>? {
        // Normalize to the PropertyName (aqlType=ID) element
        val idElement: AqlNamedElement = when {
            sourceEl is AqlNamedElement && sourceEl.aqlType == AqlMixinType.ID ->
                sourceEl
            sourceEl.parent is AqlNamedElement &&
                    (sourceEl.parent as AqlNamedElement).aqlType == AqlMixinType.ID ->
                sourceEl.parent as AqlNamedElement
            else -> return null
        }

        val name = idElement.name?.trim('`')?.takeIf { it.isNotBlank() } ?: return null

        // Must be inside language injection (i.e., inside a @Query Java string)
        val injectionHost = InjectedLanguageManager.getInstance(sourceEl.project)
            .getInjectionHost(sourceEl) ?: return null

        // Dispatch based on PSI parent:
        //   PropertyLookup parent → c.surname (field access)
        //   Any other parent      → characters (collection reference)
        val lookupParent = idElement.parent
        val isPropertyAccess = lookupParent is AqlNamedElement &&
                lookupParent.aqlType == AqlMixinType.PROPERTY_LOOKUP

        return resolveFromHost(injectionHost, name, isPropertyAccess)
    }

    // ─── Path B: Java literal text analysis ───────────────────────────────────

    private fun javaLiteralPath(sourceEl: PsiElement, offset: Int): Array<PsiElement>? {
        val literal: PsiLiteralExpression = when {
            sourceEl is PsiLiteralExpression        -> sourceEl
            sourceEl.parent is PsiLiteralExpression -> sourceEl.parent as PsiLiteralExpression
            else                                    -> return null
        }
        if (literal.value !is String) return null

        // Verify the literal is inside a @Query annotation before doing any text work
        val annotation = PsiTreeUtil.getParentOfType(literal, PsiAnnotation::class.java)
            ?: return null
        if (annotation.qualifiedName != QUERY_ANNOTATION) return null

        val text    = literal.text               // includes surrounding quotes
        val content = text.substring(1, text.length - 1)  // raw string content
        val posInContent = offset - literal.textRange.startOffset - 1
        if (posInContent < 0 || posInContent >= content.length) return null

        val word = wordAt(content, posInContent) ?: return null
        val isPropAccess = isPrecededByDot(content, posInContent)

        return resolveFromHost(literal, word, isPropAccess)
    }

    // ─── Shared resolution ────────────────────────────────────────────────────

    /**
     * Given a [host] element (the injection host `PsiLiteralExpression`) and an [identifier]
     * extracted from the AQL, resolves either a field in the entity class ([isPropertyAccess]=true)
     * or an entity class by collection name ([isPropertyAccess]=false).
     */
    private fun resolveFromHost(
        host: PsiElement,
        identifier: String,
        isPropertyAccess: Boolean
    ): Array<PsiElement>? {
        val annotation = PsiTreeUtil.getParentOfType(host, PsiAnnotation::class.java)
            ?: return null
        if (annotation.qualifiedName != QUERY_ANNOTATION) return null

        return if (isPropertyAccess) {
            // c.surname → field in entity class
            val method     = PsiTreeUtil.getParentOfType(annotation, PsiMethod::class.java) ?: return null
            val repository = method.containingClass ?: return null
            val entity     = resolveEntityClass(repository) ?: return null
            findEntityField(entity, identifier)
        } else {
            // characters → @Document entity class
            findDocumentClass(host.project, identifier)
        }
    }

    // ─── Entity-class resolution ──────────────────────────────────────────────

    /**
     * Extracts the entity class (first type parameter) from a Spring Data repository.
     * `CharacterRepository extends ArangoRepository<Character, String>` → `Character`.
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
     * Finds the Java field in [entityClass] (and its superclass hierarchy) matching [fieldName].
     *
     * Resolution order:
     *  1. Direct Java field name
     *  2. `@Field("fieldName")` annotation on any field
     */
    private fun findEntityField(entityClass: PsiClass, fieldName: String): Array<PsiElement>? {
        entityClass.findFieldByName(fieldName, true)?.let { return arrayOf(it) }

        val annotated = entityClass.allFields.firstOrNull { field ->
            field.getAnnotation(FIELD_ANNOTATION)
                ?.findDeclaredAttributeValue("value")
                ?.let { (it as? PsiLiteralExpression)?.value as? String } == fieldName
        }
        return if (annotated != null) arrayOf(annotated) else null
    }

    /**
     * Searches the project for a `@Document`-annotated class whose resolved collection
     * name equals [collectionName].
     *
     * Considers both explicit `@Document("name")` values and the default lower-camelCase
     * derivation from the class name.
     */
    private fun findDocumentClass(project: Project, collectionName: String): Array<PsiElement>? {
        val annotationClass = JavaPsiFacade.getInstance(project)
            .findClass(DOCUMENT_ANNOTATION, GlobalSearchScope.allScope(project))
            ?: return null

        val match = AnnotatedElementsSearch
            .searchPsiClasses(annotationClass, GlobalSearchScope.projectScope(project))
            .findAll()
            .firstOrNull { cls -> resolveDocumentCollectionName(cls) == collectionName }
            ?: return null

        return arrayOf(match)
    }

    /**
     * Returns the ArangoDB collection name for [cls]:
     *  - explicit `@Document("name")` value if present
     *  - lower-camelCase class name otherwise (Spring Data ArangoDB default)
     */
    private fun resolveDocumentCollectionName(cls: PsiClass): String {
        val annotation = cls.getAnnotation(DOCUMENT_ANNOTATION)
        val explicit = annotation
            ?.findDeclaredAttributeValue("value")
            ?.let { (it as? PsiLiteralExpression)?.value as? String }
            ?.takeIf { it.isNotBlank() }
        return explicit ?: cls.name?.replaceFirstChar { it.lowercaseChar() } ?: ""
    }

    // ─── Text-analysis helpers (Path B) ───────────────────────────────────────

    /**
     * Returns the identifier word containing position [pos] in [content], or `null`
     * if [pos] is not on a word character.
     */
    private fun wordAt(content: String, pos: Int): String? {
        if (pos < 0 || pos >= content.length) return null
        val c = content[pos]
        if (!c.isLetterOrDigit() && c != '_') return null

        var start = pos
        while (start > 0 && (content[start - 1].isLetterOrDigit() || content[start - 1] == '_')) {
            start--
        }
        var end = pos
        while (end < content.length - 1 &&
               (content[end + 1].isLetterOrDigit() || content[end + 1] == '_')
        ) {
            end++
        }
        return content.substring(start, end + 1).takeIf { it.isNotBlank() }
    }

    /**
     * Returns `true` if the word that contains [pos] in [content] is immediately
     * preceded by a `.` character (i.e., it is a property-access expression).
     */
    private fun isPrecededByDot(content: String, pos: Int): Boolean {
        var start = pos
        while (start > 0 && (content[start - 1].isLetterOrDigit() || content[start - 1] == '_')) {
            start--
        }
        return start > 0 && content[start - 1] == '.'
    }
}
