package com.arangodb.intellij.aql.spring

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression

/**
 * Inspection that warns when a Spring Data `@Document` annotation references a collection
 * that does not exist in the currently active ArangoDB database.
 *
 * Only fires when a live connection is established (i.e. the schema cache is populated).
 * When not connected the inspection is silent to avoid false positives.
 *
 * The highlighted element is the string value inside `@Document("...")` when explicit,
 * or the annotation itself when the collection name is derived from the class name.
 */
class SpringDataDocumentInspection : LocalInspectionTool() {

    companion object {
        private const val DOCUMENT_ANNOTATION = "com.arangodb.springframework.annotation.Document"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (annotation.qualifiedName != DOCUMENT_ANNOTATION) return

                val cls = annotation.owner as? PsiClass ?: return
                val collectionName = resolveCollectionName(annotation, cls)
                if (collectionName.isBlank()) return

                val service = holder.project.getService(AqlDatabaseService::class.java)
                // Do not warn when not connected — cache is empty
                if (service.getAll().isEmpty()) return

                if (service.getCollections().none { it.lookupString == collectionName }) {
                    val target = annotation.findDeclaredAttributeValue("value") ?: annotation
                    holder.registerProblem(
                        target,
                        "ArangoDB collection '$collectionName' not found in active database",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
        }

    private fun resolveCollectionName(annotation: PsiAnnotation, cls: PsiClass): String {
        val explicit = annotation.findDeclaredAttributeValue("value")
            ?.let { (it as? PsiLiteralExpression)?.value as? String }
            ?.takeIf { it.isNotBlank() }
        return explicit ?: cls.name?.replaceFirstChar { it.lowercaseChar() } ?: ""
    }
}
