package com.arangodb.intellij.aql.ui.panels

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JPanel
import kotlin.math.*

/**
 * Swing panel that renders ArangoDB graph traversal results.
 *
 * Parses JSON result for objects containing {vertex, edge} pairs, raw edges (_from/_to),
 * or vertex documents (_id). Lays them out with a force-directed algorithm and allows
 * dragging individual nodes.
 */
class AqlGraphPanel : JPanel() {

    data class GraphNode(val id: String, val label: String)
    data class GraphEdge(val from: String, val to: String)

    private val nodes = mutableListOf<GraphNode>()
    private val edges = mutableListOf<GraphEdge>()
    private val positions = mutableMapOf<String, Point2D.Float>()

    private var draggingId: String? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    init {
        preferredSize = JBUI.size(600, 400)
        background = JBColor.background()

        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                draggingId = hitTest(e.x, e.y)
                draggingId?.let { id ->
                    val p = positions[id] ?: return
                    dragOffsetX = e.x - p.x.toInt()
                    dragOffsetY = e.y - p.y.toInt()
                }
            }
            override fun mouseReleased(e: MouseEvent) { draggingId = null }
            override fun mouseDragged(e: MouseEvent) {
                val id = draggingId ?: return
                positions[id] = Point2D.Float(
                    (e.x - dragOffsetX).toFloat(),
                    (e.y - dragOffsetY).toFloat()
                )
                repaint()
            }
        }
        addMouseListener(mouse)
        addMouseMotionListener(mouse)
    }

    fun setData(jsonResult: String) {
        nodes.clear()
        edges.clear()
        positions.clear()
        parseGraph(jsonResult)
        if (nodes.isNotEmpty()) layoutForce()
        repaint()
    }

    fun clearData() {
        nodes.clear(); edges.clear(); positions.clear(); repaint()
    }

    // ─── Parsing ────────────────────────────────────────────────────────────

    private fun parseGraph(json: String) {
        val mapper = ObjectMapper()
        val nodeMap = linkedMapOf<String, GraphNode>()
        val edgeSet = linkedSetOf<GraphEdge>()

        fun ensureNode(id: String, key: String? = null) {
            nodeMap.getOrPut(id) { GraphNode(id, (key ?: id).substringAfter("/").take(14)) }
        }

        fun processEdge(n: JsonNode) {
            val from = n["_from"]?.asText() ?: return
            val to = n["_to"]?.asText() ?: return
            ensureNode(from)
            ensureNode(to)
            edgeSet.add(GraphEdge(from, to))
        }

        fun processVertex(n: JsonNode) {
            val id = n["_id"]?.asText() ?: return
            ensureNode(id, n["_key"]?.asText())
        }

        try {
            val root = mapper.readTree(json)
            val items: Iterable<JsonNode> = if (root.isArray) root else listOf(root)
            for (item in items) {
                when {
                    item.has("vertex") && item.has("edge") -> {
                        item["vertex"]?.let { processVertex(it) }
                        item["edge"]?.let { processEdge(it) }
                    }
                    item.has("_from") && item.has("_to") -> processEdge(item)
                    item.has("_id") -> processVertex(item)
                }
            }
        } catch (_: Exception) {}

        nodes.addAll(nodeMap.values)
        edges.addAll(edgeSet)
    }

    // ─── Layout ─────────────────────────────────────────────────────────────

    private fun layoutForce() {
        val w = width.takeIf { it > 50 } ?: 600
        val h = height.takeIf { it > 50 } ?: 400
        val pad = JBUI.scale(60f)
        val n = nodes.size

        // Seed: circular arrangement
        nodes.forEachIndexed { i, node ->
            val angle = 2 * PI * i / n - PI / 2
            val r = (min(w, h) / 2.5f - pad).coerceAtLeast(30f)
            positions[node.id] = Point2D.Float(
                w / 2f + r * cos(angle).toFloat(),
                h / 2f + r * sin(angle).toFloat()
            )
        }
        if (n <= 1) return

        val repK = sqrt((w * h).toFloat() / n) * 1.4f
        val attK = repK * 0.5f
        var temp = min(w, h) * 0.25f

        repeat(80) {
            val fx = FloatArray(n)
            val fy = FloatArray(n)

            // Repulsion
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val pi = positions[nodes[i].id] ?: continue
                    val pj = positions[nodes[j].id] ?: continue
                    val dx = pi.x - pj.x
                    val dy = pi.y - pj.y
                    val d = max(sqrt((dx * dx + dy * dy).toDouble()).toFloat(), 1f)
                    val rep = repK * repK / d
                    fx[i] += rep * dx / d;  fy[i] += rep * dy / d
                    fx[j] -= rep * dx / d;  fy[j] -= rep * dy / d
                }
            }

            // Attraction
            for (edge in edges) {
                val fi = nodes.indexOfFirst { it.id == edge.from }.takeIf { it >= 0 } ?: continue
                val ti = nodes.indexOfFirst { it.id == edge.to }.takeIf { it >= 0 } ?: continue
                val pf = positions[edge.from] ?: continue
                val pt = positions[edge.to] ?: continue
                val dx = pt.x - pf.x
                val dy = pt.y - pf.y
                val d = max(sqrt((dx * dx + dy * dy).toDouble()).toFloat(), 1f)
                val att = d * d / attK
                fx[fi] += att * dx / d;  fy[fi] += att * dy / d
                fx[ti] -= att * dx / d;  fy[ti] -= att * dy / d
            }

            // Apply with cooling
            for (i in 0 until n) {
                val pos = positions[nodes[i].id] ?: continue
                val len = max(sqrt((fx[i] * fx[i] + fy[i] * fy[i]).toDouble()).toFloat(), 0.001f)
                val move = min(len, temp)
                pos.x = (pos.x + fx[i] / len * move).coerceIn(pad, w - pad)
                pos.y = (pos.y + fy[i] / len * move).coerceIn(pad, h - pad)
            }
            temp *= 0.92f
        }
    }

    private fun hitTest(x: Int, y: Int): String? {
        val r = NODE_R
        return nodes.firstOrNull { node ->
            val p = positions[node.id] ?: return@firstOrNull false
            val dx = x - p.x; val dy = y - p.y
            dx * dx + dy * dy <= r * r
        }?.id
    }

    // ─── Painting ────────────────────────────────────────────────────────────

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        if (nodes.isEmpty()) {
            paintEmpty(g2); return
        }
        if (positions.isEmpty()) layoutForce()

        paintEdges(g2)
        paintNodes(g2)
    }

    private fun paintEmpty(g2: Graphics2D) {
        g2.color = JBColor.GRAY
        val msg = "No graph data — execute a traversal query to visualize results here."
        val fm = g2.fontMetrics
        g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2)
    }

    private fun paintEdges(g2: Graphics2D) {
        g2.color = EDGE_COLOR
        g2.stroke = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        for (edge in edges) {
            val f = positions[edge.from] ?: continue
            val t = positions[edge.to] ?: continue
            paintArrow(g2, f, t)
        }
    }

    private fun paintArrow(g2: Graphics2D, from: Point2D.Float, to: Point2D.Float) {
        val dx = to.x - from.x; val dy = to.y - from.y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist < NODE_R * 2 + 4) return
        val startRatio = (NODE_R + 3f) / dist
        val endRatio = (dist - NODE_R - 3f) / dist
        val sx = from.x + dx * startRatio; val sy = from.y + dy * startRatio
        val ex = from.x + dx * endRatio;   val ey = from.y + dy * endRatio
        g2.drawLine(sx.toInt(), sy.toInt(), ex.toInt(), ey.toInt())
        // Arrowhead
        val angle = atan2(dy.toDouble(), dx.toDouble())
        val al = 10.0; val aa = PI / 6
        val x1 = ex - al * cos(angle - aa); val y1 = ey - al * sin(angle - aa)
        val x2 = ex - al * cos(angle + aa); val y2 = ey - al * sin(angle + aa)
        g2.fillPolygon(
            intArrayOf(ex.toInt(), x1.toInt(), x2.toInt()),
            intArrayOf(ey.toInt(), y1.toInt(), y2.toInt()), 3
        )
    }

    private fun paintNodes(g2: Graphics2D) {
        val r = NODE_R
        val fm = g2.getFontMetrics(NODE_FONT)
        g2.font = NODE_FONT
        for (node in nodes) {
            val p = positions[node.id] ?: continue
            val x = p.x.toInt(); val y = p.y.toInt()
            // Shadow
            g2.color = SHADOW_COLOR
            g2.fillOval(x - r + 2, y - r + 2, r * 2, r * 2)
            // Fill
            g2.color = NODE_FILL
            g2.fillOval(x - r, y - r, r * 2, r * 2)
            // Border
            g2.color = NODE_BORDER
            g2.stroke = BasicStroke(1.5f)
            g2.drawOval(x - r, y - r, r * 2, r * 2)
            // Label
            g2.color = LABEL_COLOR
            val lbl = node.label
            g2.drawString(lbl, x - fm.stringWidth(lbl) / 2, y + fm.ascent / 3)
        }
    }

    companion object {
        private val NODE_R = JBUI.scale(22)
        private val NODE_FONT = Font(Font.SANS_SERIF, Font.BOLD, JBUI.scale(11))
        private val NODE_FILL   = JBColor(Color(70, 130, 180), Color(80, 140, 200))
        private val NODE_BORDER = JBColor(Color(45, 95, 140), Color(55, 110, 160))
        private val EDGE_COLOR  = JBColor(Color(130, 130, 130), Color(160, 160, 160))
        private val SHADOW_COLOR = JBColor(Color(0, 0, 0, 25), Color(0, 0, 0, 45))
        private val LABEL_COLOR = JBColor.WHITE
    }
}
