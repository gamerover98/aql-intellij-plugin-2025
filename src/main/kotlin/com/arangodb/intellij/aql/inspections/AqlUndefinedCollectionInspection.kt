package com.arangodb.intellij.aql.inspections

import com.arangodb.intellij.aql.db.AqlDatabaseService
import com.arangodb.intellij.aql.grammar.custom.psi.AqlMixinType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class AqlUndefinedCollectionInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is AqlNamedElement) return
                if (element.aqlType != AqlMixinType.ID) return
                val service = element.project.getService(AqlDatabaseService::class.java)
                val known = service.getAll()
                // Only warn when connected (cache populated)
                if (known.isEmpty()) return
                val name = element.text
                if (known.none { it.lookupString == name }) {
                    holder.registerProblem(
                        element,
                        "Unknown ArangoDB collection, graph, or view: '$name'",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
        }
}
