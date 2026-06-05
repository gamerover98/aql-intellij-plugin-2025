package com.arangodb.intellij.aql.actions

import com.arangodb.intellij.aql.services.AqlResultService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.nio.charset.StandardCharsets

class AqlExportResultsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val content = project.getService(AqlResultService::class.java).lastResult
        if (content.isBlank()) return
        saveToFile(project, content)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: run { e.presentation.isEnabled = false; return }
        val hasResult = project.getService(AqlResultService::class.java).lastResult.isNotBlank()
        e.presentation.isEnabled = hasResult
    }

    private fun saveToFile(project: Project, content: String) {
        val descriptor = FileSaverDescriptor("Export Query Results", "Save results as JSON file", "json")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val wrapper = dialog.save(null as VirtualFile?, "query_results") ?: return
        try {
            val file = wrapper.getVirtualFile(true) ?: return
            file.setBinaryContent(content.toByteArray(StandardCharsets.UTF_8))
        } catch (_: IOException) {
        }
    }
}
