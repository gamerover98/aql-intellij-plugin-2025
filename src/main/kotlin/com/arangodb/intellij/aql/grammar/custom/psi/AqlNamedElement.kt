package com.arangodb.intellij.aql.grammar.custom.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

interface AqlNamedElement : PsiNameIdentifierOwner, NavigatablePsiElement, PsiNamedElement {
    val aqlType: AqlMixinType
}
