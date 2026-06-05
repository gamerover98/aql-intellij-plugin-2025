package com.arangodb.intellij.aql.templates

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class AqlTemplateProvider : DefaultLiveTemplatesProvider {

    override fun getDefaultLiveTemplateFiles(): Array<String> =
        arrayOf("/liveTemplates/aql_document")

    override fun getHiddenLiveTemplateFiles(): Array<String>? = null
}
