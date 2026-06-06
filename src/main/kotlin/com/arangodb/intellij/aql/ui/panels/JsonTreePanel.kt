package com.arangodb.intellij.aql.ui.panels

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * A collapsible, color-coded JSON tree viewer for AQL query results.
 *
 * - Strings → green  |  Numbers → blue  |  Booleans → orange  |  null → gray
 * - Object/array container nodes show a summary label ({N fields} / [N items])
 * - `_class` values are rendered as clickable hyperlinks that navigate to the
 *   matching Java/Kotlin class in the project (requires com.intellij.modules.java).
 */
class JsonTreePanel(private val project: Project) : JPanel(BorderLayout()) {

    /** Payload stored in each tree node's userObject. */
    private data class Entry(val key: String, val node: JsonNode)

    private val rootNode = DefaultMutableTreeNode("root")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val mapper = ObjectMapper()

    init {
        background = JBColor.background()

        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.border = JBUI.Borders.empty(4, 6)
        tree.cellRenderer = JsonCellRenderer()

        // Navigate to class on _class node click; update cursor on hover
        val mouseHandler = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val path = tree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val entry = node.userObject as? Entry ?: return
                if (entry.key == "_class" && entry.node.isTextual) {
                    navigateToClass(entry.node.asText())
                }
            }
            override fun mouseMoved(e: MouseEvent) {
                val path = tree.getPathForLocation(e.x, e.y)
                val entry = (path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Entry
                tree.cursor = if (entry?.key == "_class" && entry.node.isTextual)
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                else
                    Cursor.getDefaultCursor()
            }
        }
        tree.addMouseListener(mouseHandler)
        tree.addMouseMotionListener(mouseHandler)

        add(JBScrollPane(tree), BorderLayout.CENTER)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setData(json: String) {
        rootNode.removeAllChildren()
        if (json.isNotBlank()) {
            try {
                addChildren(rootNode, mapper.readTree(json))
            } catch (_: Exception) { /* malformed JSON — leave tree empty */ }
        }
        treeModel.reload()
        // Ensure root stays expanded so its children are visible
        tree.expandPath(TreePath(rootNode))
    }

    fun clear() {
        rootNode.removeAllChildren()
        treeModel.reload()
    }

    // ─── Tree construction ────────────────────────────────────────────────────

    private fun addChildren(parent: DefaultMutableTreeNode, node: JsonNode) {
        when {
            node.isObject -> node.fields().forEach { (key, value) ->
                val child = DefaultMutableTreeNode(Entry(key, value))
                parent.add(child)
                if (value.isContainerNode) addChildren(child, value)
            }
            node.isArray -> node.forEachIndexed { i, value ->
                val child = DefaultMutableTreeNode(Entry("[$i]", value))
                parent.add(child)
                if (value.isContainerNode) addChildren(child, value)
            }
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    private fun navigateToClass(fqn: String) {
        val shortName = fqn.trim().substringAfterLast('.')
        val scope = GlobalSearchScope.projectScope(project)
        // Run file search off EDT; navigate back on EDT
        ApplicationManager.getApplication().executeOnPooledThread {
            val vf: VirtualFile? = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
                FilenameIndex.getVirtualFilesByName("$shortName.java", scope).firstOrNull()
                    ?: FilenameIndex.getVirtualFilesByName("$shortName.kt", scope).firstOrNull()
            }
            vf?.let { ApplicationManager.getApplication().invokeLater { OpenFileDescriptor(project, it).navigate(true) } }
        }
    }

    // ─── Cell renderer ────────────────────────────────────────────────────────

    private inner class JsonCellRenderer : DefaultTreeCellRenderer() {

        override fun getTreeCellRendererComponent(
            tree: javax.swing.JTree, value: Any, selected: Boolean,
            expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
            icon = null
            val entry = (value as? DefaultMutableTreeNode)?.userObject as? Entry
            if (entry != null) text = buildHtml(entry, selected)
            return this
        }

        private fun buildHtml(e: Entry, selected: Boolean): String {
            val kc = if (selected) "inherit" else keyColor()
            val k  = esc(e.key)
            return when {
                e.node.isTextual -> {
                    val isClass = e.key == "_class"
                    val vc = if (selected) "inherit" else if (isClass) classLinkColor() else strColor()
                    val raw = esc(e.node.asText()).take(200)
                    val display = if (isClass) "<u>$raw</u>" else "&quot;$raw&quot;"
                    html(kc, k, vc, display)
                }
                e.node.isNumber ->
                    html(kc, k, if (selected) "inherit" else numColor(), esc(e.node.asText()))
                e.node.isBoolean ->
                    html(kc, k, if (selected) "inherit" else boolColor(), esc(e.node.asText()))
                e.node.isNull ->
                    html(kc, k, if (selected) "inherit" else nullColor(), "null")
                e.node.isObject -> {
                    val n = e.node.size()
                    html(kc, k, "gray", "{$n field${if (n != 1) "s" else ""}}")
                }
                e.node.isArray -> {
                    val n = e.node.size()
                    html(kc, k, "gray", "[$n item${if (n != 1) "s" else ""}]")
                }
                else -> "<html><span color='$kc'>$k</span>: ${esc(e.node.asText())}</html>"
            }
        }

        private fun html(kc: String, k: String, vc: String, v: String) =
            "<html><span color='$kc'>$k</span>: <span color='$vc'>$v</span></html>"

        private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        private fun dark() = !JBColor.isBright()
        private fun strColor()       = if (dark()) "#6AAB73" else "#007B00"
        private fun numColor()       = if (dark()) "#6897BB" else "#0000CC"
        private fun boolColor()      = if (dark()) "#CC7832" else "#0000AA"
        private fun nullColor()      = "#888888"
        private fun keyColor()       = if (dark()) "#D4D4D4" else "#1A1A1A"
        private fun classLinkColor() = if (dark()) "#4A9EFF" else "#0066CC"
    }
}
