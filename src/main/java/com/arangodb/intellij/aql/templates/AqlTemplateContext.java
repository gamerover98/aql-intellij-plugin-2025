package com.arangodb.intellij.aql.templates;

import org.jetbrains.annotations.NotNull;

import com.arangodb.intellij.aql.fileTypes.AqlFileType;
import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;

public class AqlTemplateContext extends TemplateContextType {
    protected AqlTemplateContext() {
        super("AQL", "AQL");
    }

    @Override
    public boolean isInContext(@NotNull final TemplateActionContext templateActionContext) {
        return templateActionContext.getFile().getFileType().equals(AqlFileType.INSTANCE);
    }
}
