package com.arangodb.intellij.aql.templates

import com.arangodb.intellij.aql.fileTypes.AqlFileType
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class AqlTemplateContext : TemplateContextType("AQL", "AQL") {

    override fun isInContext(templateActionContext: TemplateActionContext): Boolean =
        templateActionContext.file.fileType == AqlFileType
}
