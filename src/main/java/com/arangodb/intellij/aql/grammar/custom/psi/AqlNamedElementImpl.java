package com.arangodb.intellij.aql.grammar.custom.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.ContributedReferenceHost;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AqlNamedElementImpl extends ASTWrapperPsiElement implements ContributedReferenceHost, AqlNamedElement {


    public AqlNamedElementImpl(@NotNull final ASTNode node) {
        super(node);
    }


    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public PsiElement setName(@NlsSafe @NotNull String s) throws IncorrectOperationException {
        return null; //TODO: added to fix compiler error, implement if needed.
    }
}
