package com.arangodb.intellij.aql.util

import com.arangodb.entity.AqlExecutionExplainEntity
import com.arangodb.intellij.aql.fileTypes.AqlFile
import com.arangodb.intellij.aql.fileTypes.AqlFileType
import com.arangodb.intellij.aql.grammar.custom.psi.AqlNamedElement
import com.arangodb.intellij.aql.grammar.generated.psi.AqlParameterVariable
import com.arangodb.intellij.aql.model.AqlQuery
import com.google.common.base.CharMatcher
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.primitives.Ints
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import java.util.Properties

object AqlUtils {
    private const val NAME_TEMPLATE_PROPERTY = "NAME"
    private const val LOW_CASE_NAME_TEMPLATE_PROPERTY = "lowCaseName"

    @JvmStatic
    @Throws(IncorrectOperationException::class)
    fun createFromTemplate(
        directory: PsiDirectory,
        name: String,
        fileName: String,
        templateName: String,
        allowReformatting: Boolean,
        vararg parameters: String
    ): PsiFile {
        val project = directory.project
        val template = FileTemplateManager.getInstance(project).getInternalTemplate(templateName)
        val properties = Properties(FileTemplateManager.getInstance(project).defaultProperties)
        properties.setProperty(NAME_TEMPLATE_PROPERTY, name)
        properties.setProperty(LOW_CASE_NAME_TEMPLATE_PROPERTY, StringUtil.decapitalize(name))
        var i = 0
        while (i < parameters.size) {
            properties.setProperty(parameters[i], parameters[i + 1])
            i += 2
        }
        val text = try {
            template.getText(properties)
        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to load template for ${FileTemplateManager.getInstance(project).internalTemplateToSubject(templateName)}",
                e
            )
        }
        return WriteAction.compute<PsiFile, RuntimeException> {
            val factory = PsiFileFactory.getInstance(project)
            var file = factory.createFileFromText(fileName, text)
            file = directory.add(file) as PsiFile
            if (allowReformatting && template.isReformatCode) {
                ReformatCodeProcessor(project, file, null, false).run()
            }
            file
        }
    }

    @JvmStatic
    fun convertToBindVariable(value: String?): Any {
        if (value == null) return ""
        return Ints.tryParse(value) ?: value
    }

    @JvmStatic
    fun createFileName(name: String, existing: Map<String, AqlQuery>): String {
        if (existing[name] == null) return name
        var idx = 1
        var newName = name + idx
        while (existing[newName] != null) {
            idx++
            newName = name + idx
        }
        return newName
    }

    @JvmStatic
    fun createDummyJsonFile(text: CharSequence, project: Project): PsiFile? = null

    @JvmStatic
    fun createDummyAqlFile(text: CharSequence, project: Project): PsiFile =
        createDummyFile(AqlFileType, text, project)

    @JvmStatic
    fun createDummyFile(fileType: LanguageFileType, text: CharSequence, project: Project): PsiFile {
        val fileName = "AQL.dummy.${fileType.defaultExtension}"
        val stamp = System.currentTimeMillis()
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, fileType, text, stamp, false)
    }

    @JvmStatic
    fun extractParameterNames(text: CharSequence, project: Project): Set<String> {
        val file = createDummyAqlFile(text, project)
        return PsiTreeUtil.collectElements(file) { it is AqlParameterVariable }
            .map { it.text }
            .toHashSet()
    }

    @JvmStatic
    fun parseExecutionEntity(entity: AqlExecutionExplainEntity): String = JSON.toJson(entity)

    @JvmStatic
    fun popupDataSourceFix(message: String, project: Project) {
        log.errorAction(message, "Fix ArangoDB data source", DataSourceWindowCallback(project))
    }

    @JvmStatic
    fun createHash(project: Project, query: String, data: Map<String, String>): String {
        val clean = CharMatcher.whitespace().removeFrom(query)
        return Hashing.sha256().newHasher().putString(clean, Charsets.UTF_8).hash().toString()
    }

    @JvmStatic
    fun findNamedElements(project: Project): List<AqlNamedElement> {
        val virtualFiles: Collection<VirtualFile> = FileTypeIndex.getFiles(AqlFileType, GlobalSearchScope.allScope(project))
        return virtualFiles.flatMap { virtualFile ->
            val aqlFile = PsiManager.getInstance(project).findFile(virtualFile) as? AqlFile ?: return@flatMap emptyList()
            PsiTreeUtil.getChildrenOfType(aqlFile, AqlNamedElement::class.java)?.toList() ?: emptyList()
        }
    }

    @JvmStatic
    fun findNamedElements(project: Project, name: String): List<AqlNamedElement> {
        val virtualFiles: Collection<VirtualFile> = FileTypeIndex.getFiles(AqlFileType, GlobalSearchScope.allScope(project))
        return virtualFiles.flatMap { virtualFile ->
            val aqlFile = PsiManager.getInstance(project).findFile(virtualFile) as? AqlFile ?: return@flatMap emptyList()
            PsiTreeUtil.getChildrenOfType(aqlFile, AqlNamedElement::class.java)
                ?.filter { it.name == name }
                ?: emptyList()
        }
    }

    @JvmStatic
    fun guessValueForParameter(name: String, element: com.intellij.psi.PsiElement): String {
        if (name.contains("limit") || name.contains("count")) return "10"
        return ""
    }

    @JvmStatic
    fun convertValues(bindVars: Map<String, String>?): Map<String, Any> {
        if (bindVars == null) return emptyMap()
        return bindVars.mapValues { (_, v) -> convertToBindVariable(v) }
    }
}
