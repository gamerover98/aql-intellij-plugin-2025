package com.arangodb.intellij.aql.spring

import com.arangodb.intellij.aql.lang.AqlLanguage
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil

/**
 * Programmatically injects the AQL language into string literals that are the value
 * of Spring Data ArangoDB `@Query` annotations.
 *
 * This gives AQL syntax highlighting, inspections, and code completion inside
 * `@Query("...")` strings without requiring the IntelliLang plugin.
 *
 * The IntelliLang XML file (`aqlCodeInjector.xml`) uses a `psiMethod()` pattern which
 * targets method call sites, NOT annotation attribute values — so it never fires.
 * This `MultiHostInjector` replaces it as the reliable injection mechanism.
 *
 * Example — AQL is highlighted and navigable inside these strings:
 * ```java
 * @Query("FOR c IN characters FILTER c.surname == @surname RETURN c")
 * List<Character> findBySurname(@Param("surname") String surname);
 * ```
 */
class AqlQueryAnnotationInjector : MultiHostInjector {

    companion object {
        private const val QUERY_ANNOTATION = "com.arangodb.springframework.annotation.Query"
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is PsiLiteralExpression) return
        if (context.value !is String) return  // skip non-string literals

        // PsiLiteralExpression implements PsiLanguageInjectionHost; the cast is safe
        val host = context as? PsiLanguageInjectionHost ?: return

        val annotation = PsiTreeUtil.getParentOfType(context, PsiAnnotation::class.java)
            ?: return
        if (annotation.qualifiedName != QUERY_ANNOTATION) return

        val text = context.text
        // text includes surrounding quotes; minimum valid string is "" (2 chars)
        if (text.length < 2) return

        // Strip the opening and closing quote characters
        registrar
            .startInjecting(AqlLanguage)
            .addPlace(null, null, host, TextRange.create(1, text.length - 1))
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(PsiLiteralExpression::class.java)
}
