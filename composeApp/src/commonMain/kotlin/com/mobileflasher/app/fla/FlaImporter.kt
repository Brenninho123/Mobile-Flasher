package com.mobileflasher.app.fla

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.xml.XmlNode
import com.mobileflasher.app.xml.parseXml
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal class FlaImporter(private val entries: Map<String, ByteArray>) {

    private var skipped = 0
    private var shapeCounter = 0
    private var layerCounter = 0

    private val symbols: Map<String, XmlNode> by lazy {
        val found = LinkedHashMap<String, XmlNode>()
        for ((path, data) in entries) {
            if (!path.startsWith(LibraryPrefix) || !path.endsWith(".xml")) continue
            val node = try {
                parseXml(data.decodeToString())
            } catch (e: Exception) {
                continue
            }
            val name = node.attributes["name"]
            if (node.name == "DOMSymbolItem" && name != null) found[name] = node
        }
        found
    }

    fun run(): FlaImport {
        val documentBytes = entries[DocumentEntry] ?: throw IllegalArgumentException("The .fla file has no DOMDocument.xml")
        val document = parseXml(documentBytes.decodeToString())
        require(document.name == "DOMDocument") { "Not an Adobe Flash document" }
        val frameRate = document.attributes["frameRate"]?.toFloatOrNull()?.toInt()?.coerceIn(1, 60) ?: 24
        val timeline = document.child("timelines")?.childrenNamed("DOMTimeline")?.firstOrNull()
        val layers = timeline?.let { importLayers(it, Affine(), 0) }.orEmpty()
        val finalLayers = layers.ifEmpty { listOf(Layer(id = "fla-layer-0", name = "Layer 1")) }
        val frameCount = finalLayers.maxOf { it.frames.size }
        return FlaImport(
            project = Project(
                name = DefaultName,
                frameRate = frameRate,
                layers = finalLayers,
                mode = if (frameCount > 1) ProjectMode.ANIMATION else ProjectMode.ART
            ),
            skippedElements = skipped
        )
    }

    private fun importLayers(timeline: XmlNode, matrix: Affine, depth: Int): List<Layer> {
        val domLayers = timeline.child("layers")?.childrenNamed("DOMLayer").orEmpty()
        val result = ArrayList<Layer>()
        for (domLayer in domLayers.asReversed()) {
            val type = domLayer.attributes["layerType"]
            if (type == "folder" || type == "guide" || type == "mask") {
                continue
            }
            val frames = importFrames(domLayer, matrix, depth, firstFrameOnly = depth > 0)
            if (frames.isEmpty()) continue
            result.add(
                Layer(
                    id = "fla-layer-${layerCounter++}",
                    name = domLayer.attributes["name"] ?: "Layer ${layerCounter}",
                    isVisible = domLayer.attributes["visible"] != "false",
                    isLocked = domLayer.attributes["locked"] == "true",
                    frames = frames
                )
            )
        }
        return result
    }

    private fun importFrames(domLayer: XmlNode, matrix: Affine, depth: Int, firstFrameOnly: Boolean): List<Frame> {
        val domFrames = domLayer.child("frames")?.childrenNamed("DOMFrame").orEmpty()
        if (firstFrameOnly) {
            val first = domFrames.firstOrNull() ?: return emptyList()
            return listOf(Frame(index = 0, isKeyframe = true, shapes = elementShapes(first.child("elements"), matrix, depth)))
        }
        val frames = ArrayList<Frame>()
        for (domFrame in domFrames) {
            val index = domFrame.attributes["index"]?.toIntOrNull() ?: frames.size
            val duration = (domFrame.attributes["duration"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
            if (index >= MaxFrames) break
            while (frames.size < index) frames.add(Frame(index = frames.size, isKeyframe = false, shapes = emptyList()))
            val shapes = elementShapes(domFrame.child("elements"), matrix, depth)
            frames.add(Frame(index = frames.size, isKeyframe = true, shapes = shapes))
            repeat(min(duration - 1, MaxFrames - frames.size)) {
                frames.add(Frame(index = frames.size, isKeyframe = false, shapes = shapes))
            }
        }
        return frames
    }

    private fun elementShapes(elements: XmlNode?, parent: Affine, depth: Int): List<VectorShape> {
        if (elements == null) return emptyList()
        val shapes = ArrayList<VectorShape>()
        for (element in elements.children) {
            val matrix = parent.then(matrixOf(element))
            when (element.name) {
                "DOMShape" -> shapes.addAll(importShape(element, matrix))
                "DOMRectangle" -> shapes.addAll(importPrimitive(element, matrix, oval = false))
                "DOMOvalPrimitive" -> shapes.addAll(importPrimitive(element, matrix, oval = true))
                "DOMGroup" -> shapes.addAll(elementShapes(element.child("members"), matrix, depth))
                "DOMSymbolInstance" -> shapes.addAll(importSymbol(element, matrix, depth))
                else -> skipped++
            }
        }
        return shapes
    }

    private fun importSymbol(instance: XmlNode, matrix: Affine, depth: Int): List<VectorShape> {
        if (depth >= MaxSymbolDepth) {
            skipped++
            return emptyList()
        }
        val symbol = instance.attributes["libraryItemName"]?.let { symbols[it] }
        val timeline = symbol?.child("timeline")?.childrenNamed("DOMTimeline")?.firstOrNull()
        if (timeline == null) {
            skipped++
            return emptyList()
        }
        return importLayers(timeline, matrix, depth + 1).flatMap { layer -> layer.frames.firstOrNull()?.shapes.orEmpty() }
    }

    private fun importPrimitive(node: XmlNode, matrix: Affine, oval: Boolean): List<VectorShape> {
        val x = node.attributes["x"]?.toFloatOrNull() ?: 0f
        val y = node.attributes["y"]?.toFloatOrNull() ?: 0f
        val width = node.attributes["objectWidth"]?.toFloatOrNull() ?: return emptyList()
        val height = node.attributes["objectHeight"]?.toFloatOrNull() ?: return emptyList()
        val fill = node.child("fill")?.let { colorOf(it) }
        val stroke = strokeOf(node.child("stroke"))
        val strokeColor = stroke?.first ?: Color.Transparent
        val strokeWidth = (stroke?.second ?: 0f) * matrix.scaleFactor

        if (matrix.isAxisAligned) {
            val a = matrix.apply(Offset(x, y))
            val b = matrix.apply(Offset(x + width, y + height))
            val topLeft = Offset(min(a.x, b.x), min(a.y, b.y))
            val size = Size(abs(b.x - a.x), abs(b.y - a.y))
            return listOf(
                if (oval) {
                    EllipseShape(nextId(), topLeft, size, strokeColor, strokeWidth, fill)
                } else {
                    RectangleShape(nextId(), topLeft, size, strokeColor, strokeWidth, fill)
                }
            )
        }
        val outline = if (oval) {
            List(OvalSamples) { index ->
                val angle = 2.0 * PI * index / OvalSamples
                Offset(x + width / 2f + (width / 2f) * cos(angle).toFloat(), y + height / 2f + (height / 2f) * sin(angle).toFloat())
            }
        } else {
            listOf(Offset(x, y), Offset(x + width, y), Offset(x + width, y + height), Offset(x, y + height))
        }
        return listOf(FreehandShape(nextId(), outline.map { matrix.apply(it) }, strokeColor, strokeWidth, fill, closed = true))
    }

    private fun importShape(node: XmlNode, matrix: Affine): List<VectorShape> {
        val fills = HashMap<Int, Color?>()
        node.child("fills")?.childrenNamed("FillStyle")?.forEach { style ->
            val index = style.attributes["index"]?.toIntOrNull() ?: return@forEach
            val color = colorOf(style)
            if (color == null) skipped++
            fills[index] = color
        }
        val strokes = HashMap<Int, Pair<Color, Float>?>()
        node.child("strokes")?.childrenNamed("StrokeStyle")?.forEach { style ->
            val index = style.attributes["index"]?.toIntOrNull() ?: return@forEach
            strokes[index] = strokeOf(style)
        }

        class EdgeData(val points: List<Offset>, val fillLeft: Int, val fillRight: Int, val stroke: Int)

        val edges = ArrayList<EdgeData>()
        node.child("edges")?.childrenNamed("Edge")?.forEach { edge ->
            val text = edge.attributes["edges"] ?: return@forEach
            val left = edge.attributes["fillStyle0"]?.toIntOrNull() ?: 0
            val right = edge.attributes["fillStyle1"]?.toIntOrNull() ?: 0
            val stroke = edge.attributes["strokeStyle"]?.toIntOrNull() ?: 0
            FlaEdges.parsePaths(text).forEach { path -> edges.add(EdgeData(path.map { matrix.apply(it) }, left, right, stroke)) }
        }

        val shapes = ArrayList<VectorShape>()
        for ((index, color) in fills.entries.sortedBy { it.key }) {
            if (color == null) continue
            val boundary = ArrayList<List<Offset>>()
            for (edge in edges) {
                if (edge.fillRight == index && edge.fillLeft != index) boundary.add(edge.points)
                if (edge.fillLeft == index && edge.fillRight != index) boundary.add(edge.points.asReversed())
            }
            for (chain in FlaEdges.chain(boundary)) {
                if (chain.points.size < 3) continue
                shapes.add(FreehandShape(nextId(), chain.points, Color.Transparent, 0f, color, closed = true))
            }
        }
        for ((index, style) in strokes.entries.sortedBy { it.key }) {
            if (style == null) continue
            val paths = edges.filter { it.stroke == index }.map { it.points }
            for (chain in FlaEdges.chain(paths)) {
                shapes.add(FreehandShape(nextId(), chain.points, style.first, style.second * matrix.scaleFactor, null, closed = chain.closed))
            }
        }
        return shapes
    }

    private fun colorOf(node: XmlNode): Color? {
        val source = node.findFirst { it.name == "SolidColor" } ?: node.findFirst { it.name == "GradientEntry" } ?: return null
        val hex = (source.attributes["color"] ?: "#000000").removePrefix("#")
        val rgb = hex.toLongOrNull(16)?.toInt() ?: return null
        val alpha = source.attributes["alpha"]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
        return Color(
            red = ((rgb shr 16) and 0xFF) / 255f,
            green = ((rgb shr 8) and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f,
            alpha = alpha
        )
    }

    private fun strokeOf(node: XmlNode?): Pair<Color, Float>? {
        val style = node?.findFirst { it.attributes.containsKey("weight") } ?: return null
        val color = colorOf(style) ?: return null
        return color to max(style.attributes["weight"]?.toFloatOrNull() ?: 1f, 0.5f)
    }

    private fun matrixOf(element: XmlNode): Affine {
        val node = element.child("matrix")?.childrenNamed("Matrix")?.firstOrNull() ?: return Affine()
        fun value(name: String, default: Float) = node.attributes[name]?.toFloatOrNull() ?: default
        return Affine(value("a", 1f), value("b", 0f), value("c", 0f), value("d", 1f), value("tx", 0f), value("ty", 0f))
    }

    private fun nextId(): String = "fla-shape-${shapeCounter++}"

    private fun XmlNode.child(name: String): XmlNode? = children.firstOrNull { it.name == name }

    private fun XmlNode.childrenNamed(name: String): List<XmlNode> = children.filter { it.name == name }

    private fun XmlNode.findFirst(predicate: (XmlNode) -> Boolean): XmlNode? {
        if (predicate(this)) return this
        for (child in children) child.findFirst(predicate)?.let { return it }
        return null
    }

    companion object {
        const val DocumentEntry = "DOMDocument.xml"
        const val LibraryPrefix = "LIBRARY/"
        const val DefaultName = "Untitled"
        private const val MaxFrames = 3000
        private const val MaxSymbolDepth = 6
        private const val OvalSamples = 48
    }
}
