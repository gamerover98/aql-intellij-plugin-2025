package com.arangodb.intellij.aql.grammar.custom.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.util.IncorrectOperationException

abstract class AqlNamedElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), ContributedReferenceHost, AqlNamedElement {

    override fun getReferences(): Array<PsiReference> =
        ReferenceProvidersRegistry.getReferencesFromProviders(this)

    override fun getNameIdentifier(): PsiElement = this

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = AqlPsiUtil.setName(this, name)
}
