package com.mobileflasher.app.xml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.ImageShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.platform.decodePngToImageBitmap
import kotlin.math.roundToInt

fun writeProjectXml(project: Project, assets: MutableMap<String, ByteArray>? = null): String {
    val builder = StringBuilder()
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    builder.append("<project name=\"${escapeXml(project.name)}\" frameRate=\"${project.frameRate}\">\n")
    for (layer in project.layers) {
        builder.append(
            "  <layer id=\"${escapeXml(layer.id)}\" name=\"${escapeXml(layer.name)}\" " +
                "visible=\"${layer.isVisible}\" locked=\"${layer.isLocked}\">\n"
        )
        for (frame in layer.frames) {
            builder.append("    <frame index=\"${frame.index}\" keyframe=\"${frame.isKeyframe}\">\n")
            for (shape in frame.shapes) {
                builder.append(writeShapeXml(shape, assets))
            }
            builder.append("    </frame>\n")
        }
        builder.append("  </layer>\n")
    }
    builder.append("</project>\n")
    return builder.toString()
}

fun parseProjectXml(xml: String, assets: Map<String, ByteArray> = emptyMap()): Project {
    val root = parseXml(xml)
    require(root.name == "project") { "Not a Mobile Flasher project file (root element is <${root.name}>)" }
    val name = root.attributes["name"] ?: "Untitled"
    val frameRate = root.attributes["frameRate"]?.toIntOrNull() ?: 24
    val layers = root.children.filter { it.name == "layer" }.map { parseLayerXml(it, assets) }
    return Project(
        name = name,
        frameRate = frameRate,
        layers = layers.ifEmpty { listOf(Layer(id = "layer-1", name = "Layer 1")) }
    )
}

private fun writeShapeXml(shape: VectorShape, assets: MutableMap<String, ByteArray>?): String = when (shape) {
    is RectangleShape -> "      <rect id=\"${escapeXml(shape.id)}\" x=\"${shape.topLeft.x}\" y=\"${shape.topLeft.y}\" " +
        "width=\"${shape.size.width}\" height=\"${shape.size.height}\" stroke=\"${colorToHex(shape.strokeColor)}\" " +
        "strokeWidth=\"${shape.strokeWidth}\" fill=\"${shape.fillColor?.let { colorToHex(it) } ?: ""}\" />\n"
    is EllipseShape -> "      <ellipse id=\"${escapeXml(shape.id)}\" x=\"${shape.topLeft.x}\" y=\"${shape.topLeft.y}\" " +
        "width=\"${shape.size.width}\" height=\"${shape.size.height}\" stroke=\"${colorToHex(shape.strokeColor)}\" " +
        "strokeWidth=\"${shape.strokeWidth}\" fill=\"${shape.fillColor?.let { colorToHex(it) } ?: ""}\" />\n"
    is LineShape -> "      <line id=\"${escapeXml(shape.id)}\" x1=\"${shape.start.x}\" y1=\"${shape.start.y}\" " +
        "x2=\"${shape.end.x}\" y2=\"${shape.end.y}\" stroke=\"${colorToHex(shape.strokeColor)}\" " +
        "strokeWidth=\"${shape.strokeWidth}\" />\n"
    is FreehandShape -> "      <freehand id=\"${escapeXml(shape.id)}\" stroke=\"${colorToHex(shape.strokeColor)}\" " +
        "strokeWidth=\"${shape.strokeWidth}\" points=\"${shape.points.joinToString(" ") { "${it.x},${it.y}" }}\" />\n"
    is ImageShape -> "      <image id=\"${escapeXml(shape.id)}\" x=\"${shape.topLeft.x}\" y=\"${shape.topLeft.y}\" " +
        "width=\"${shape.size.width}\" height=\"${shape.size.height}\" data=\"${base64Encode(shape.sourcePng)}\" />\n"
}

private fun parseLayerXml(node: XmlNode, assets: Map<String, ByteArray>): Layer {
    val id = node.attributes["id"] ?: "layer-0"
    val name = node.attributes["name"] ?: "Layer"
    val visible = node.attributes["visible"]?.toBooleanStrictOrNull() ?: true
    val locked = node.attributes["locked"]?.toBooleanStrictOrNull() ?: false
    val frames = node.children.filter { it.name == "frame" }.map { parseFrameXml(it, assets) }
    return Layer(
        id = id,
        name = name,
        isVisible = visible,
        isLocked = locked,
        frames = frames.ifEmpty { listOf(Frame(index = 0, isKeyframe = true)) }
    )
}

private fun parseFrameXml(node: XmlNode, assets: Map<String, ByteArray>): Frame {
    val index = node.attributes["index"]?.toIntOrNull() ?: 0
    val isKeyframe = node.attributes["keyframe"]?.toBooleanStrictOrNull() ?: true
    val shapes = node.children.mapNotNull { parseShapeXml(it, assets) }
    return Frame(index = index, isKeyframe = isKeyframe, shapes = shapes)
}

private fun parseShapeXml(node: XmlNode, assets: Map<String, ByteArray>): VectorShape? {
    val id = node.attributes["id"] ?: return null
    return when (node.name) {
        "rect" -> RectangleShape(
            id = id,
            topLeft = Offset(node.attributes.requireFloat("x"), node.attributes.requireFloat("y")),
            size = Size(node.attributes.requireFloat("width"), node.attributes.requireFloat("height")),
            strokeColor = hexToColor(node.attributes["stroke"] ?: "#FF000000"),
            strokeWidth = node.attributes["strokeWidth"]?.toFloatOrNull() ?: 2f,
            fillColor = node.attributes["fill"]?.takeIf { it.isNotBlank() }?.let { hexToColor(it) }
        )
        "ellipse" -> EllipseShape(
            id = id,
            topLeft = Offset(node.attributes.requireFloat("x"), node.attributes.requireFloat("y")),
            size = Size(node.attributes.requireFloat("width"), node.attributes.requireFloat("height")),
            strokeColor = hexToColor(node.attributes["stroke"] ?: "#FF000000"),
            strokeWidth = node.attributes["strokeWidth"]?.toFloatOrNull() ?: 2f,
            fillColor = node.attributes["fill"]?.takeIf { it.isNotBlank() }?.let { hexToColor(it) }
        )
        "line" -> LineShape(
            id = id,
            start = Offset(node.attributes.requireFloat("x1"), node.attributes.requireFloat("y1")),
            end = Offset(node.attributes.requireFloat("x2"), node.attributes.requireFloat("y2")),
            strokeColor = hexToColor(node.attributes["stroke"] ?: "#FF000000"),
            strokeWidth = node.attributes["strokeWidth"]?.toFloatOrNull() ?: 2f
        )
        "freehand" -> FreehandShape(
            id = id,
            points = (node.attributes["points"] ?: "").trim().split(" ").filter { it.isNotBlank() }.map { pair ->
                val parts = pair.split(",")
                Offset(parts[0].toFloat(), parts[1].toFloat())
            },
            strokeColor = hexToColor(node.attributes["stroke"] ?: "#FF000000"),
            strokeWidth = node.attributes["strokeWidth"]?.toFloatOrNull() ?: 2f
        )
        "image" -> {
            val assetKey = node.attributes["asset"]
            val bytes = if (assetKey != null) {
                assets[assetKey] ?: return null
            } else {
                base64Decode(node.attributes["data"] ?: return null)
            }
            ImageShape(
                id = id,
                topLeft = Offset(node.attributes.requireFloat("x"), node.attributes.requireFloat("y")),
                size = Size(node.attributes.requireFloat("width"), node.attributes.requireFloat("height")),
                image = decodePngToImageBitmap(bytes),
                sourcePng = bytes
            )
        }
        else -> null
    }
}

private fun Map<String, String>.requireFloat(key: String): Float =
    this[key]?.toFloatOrNull() ?: 0f

private fun colorToHex(color: Color): String {
    val a = (color.alpha * 255f).roundToInt() and 0xFF
    val r = (color.red * 255f).roundToInt() and 0xFF
    val g = (color.green * 255f).roundToInt() and 0xFF
    val b = (color.blue * 255f).roundToInt() and 0xFF
    val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
    return "#" + argb.toUInt().toString(16).padStart(8, '0').uppercase()
}

private fun hexToColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val argb = clean.toLong(16).toInt()
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(red = r, green = g, blue = b, alpha = a)
}

private fun imageSourceAttribute(png: ByteArray, assets: MutableMap<String, ByteArray>?): String {
    if (assets == null) return "data=\"${base64Encode(png)}\""
    val existing = assets.entries.firstOrNull { it.value === png }?.key
    val key = existing ?: "img-${assets.size}".also { assets[it] = png }
    return "asset=\"$key\""
}
