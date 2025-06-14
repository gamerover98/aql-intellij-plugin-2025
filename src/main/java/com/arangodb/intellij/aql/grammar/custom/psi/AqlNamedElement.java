package com.arangodb.intellij.aql.grammar.custom.psi;

import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;

public interface AqlNamedElement extends PsiNameIdentifierOwner, NavigatablePsiElement, PsiNamedElement {

    @NotNull
    AqlMixinType getAqlType();
}
