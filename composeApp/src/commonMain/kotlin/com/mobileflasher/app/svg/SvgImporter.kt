package com.mobileflasher.app.svg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.xml.XmlNode
import com.mobileflasher.app.xml.parseXml

fun importSvg(
    svgText: String,
    defaultStrokeColor: Color = Color.Black,
    defaultStrokeWidth: Float = 2f,
    nextId: () -> String
): List<VectorShape> {
    val root = parseXml(svgText)
    val shapes = mutableListOf<VectorShape>()
    collectShapes(root, shapes, nextId, defaultStrokeColor, defaultStrokeWidth)
    return shapes
}

private fun collectShapes(
    node: XmlNode,
    output: MutableList<VectorShape>,
    nextId: () -> String,
    strokeColor: Color,
    strokeWidth: Float
) {
    when (node.name) {
        "rect" -> {
            val x = node.attributes["x"]?.toFloatOrNull() ?: 0f
            val y = node.attributes["y"]?.toFloatOrNull() ?: 0f
            val width = node.attributes["width"]?.toFloatOrNull() ?: 0f
            val height = node.attributes["height"]?.toFloatOrNull() ?: 0f
            output.add(
                RectangleShape(
                    id = nextId(),
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    strokeColor = svgColor(node.attributes["stroke"]) ?: strokeColor,
                    strokeWidth = node.attributes["stroke-width"]?.toFloatOrNull() ?: strokeWidth,
                    fillColor = svgColor(node.attributes["fill"])
                )
            )
        }
        "circle" -> {
            val cx = node.attributes["cx"]?.toFloatOrNull() ?: 0f
            val cy = node.attributes["cy"]?.toFloatOrNull() ?: 0f
            val r = node.attributes["r"]?.toFloatOrNull() ?: 0f
            output.add(
                EllipseShape(
                    id = nextId(),
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    strokeColor = svgColor(node.attributes["stroke"]) ?: strokeColor,
                    strokeWidth = node.attributes["stroke-width"]?.toFloatOrNull() ?: strokeWidth,
                    fillColor = svgColor(node.attributes["fill"])
                )
            )
        }
        "ellipse" -> {
            val cx = node.attributes["cx"]?.toFloatOrNull() ?: 0f
            val cy = node.attributes["cy"]?.toFloatOrNull() ?: 0f
            val rx = node.attributes["rx"]?.toFloatOrNull() ?: 0f
            val ry = node.attributes["ry"]?.toFloatOrNull() ?: 0f
            output.add(
                EllipseShape(
                    id = nextId(),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    strokeColor = svgColor(node.attributes["stroke"]) ?: strokeColor,
                    strokeWidth = node.attributes["stroke-width"]?.toFloatOrNull() ?: strokeWidth,
                    fillColor = svgColor(node.attributes["fill"])
                )
            )
        }
        "line" -> {
            output.add(
                LineShape(
                    id = nextId(),
                    start = Offset(
                        node.attributes["x1"]?.toFloatOrNull() ?: 0f,
                        node.attributes["y1"]?.toFloatOrNull() ?: 0f
                    ),
                    end = Offset(
                        node.attributes["x2"]?.toFloatOrNull() ?: 0f,
                        node.attributes["y2"]?.toFloatOrNull() ?: 0f
                    ),
                    strokeColor = svgColor(node.attributes["stroke"]) ?: strokeColor,
                    strokeWidth = node.attributes["stroke-width"]?.toFloatOrNull() ?: strokeWidth
                )
            )
        }
        "polyline", "polygon" -> {
            val points = parsePointsAttribute(node.attributes["points"] ?: "")
            if (points.size > 1) {
                output.add(
                    FreehandShape(
                        id = nextId(),
                        points = if (node.name == "polygon") points + points.first() else points,
                        strokeColor = svgColor(node.attributes["stroke"]) ?: strokeColor,
                        strokeWidth = node.attributes["stroke-width"]?.toFloatOrNull() ?: strokeWidth
                    )
                )
            }
        }
        "path" -> {
            val points = parseSimplePathData(node.attributes["d"] ?: "")
            if (points.size > 1) {
                output.add(
                    FreehandShape(
                        id = nextId(),
                        points = points,
                        strokeColor = svgColor(node.attributes["stroke"]) ?: strokeColor,
                        strokeWidth = node.attributes["stroke-width"]?.toFloatOrNull() ?: strokeWidth
                    )
                )
            }
        }
    }
    for (child in node.children) {
        collectShapes(child, output, nextId, strokeColor, strokeWidth)
    }
}

private fun parsePointsAttribute(raw: String): List<Offset> {
    return raw.trim()
        .split(Regex("[\\s,]+"))
        .filter { it.isNotBlank() }
        .mapNotNull { it.toFloatOrNull() }
        .chunked(2)
        .filter { it.size == 2 }
        .map { Offset(it[0], it[1]) }
}

private fun parseSimplePathData(d: String): List<Offset> {
    val points = mutableListOf<Offset>()
    val tokens = Regex("[MLZ]|-?\\d*\\.?\\d+(?:[eE][+-]?\\d+)?", RegexOption.IGNORE_CASE)
        .findAll(d)
        .map { it.value }
        .toList()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        when {
            token.equals("M", ignoreCase = true) || token.equals("L", ignoreCase = true) -> {
                i++
                if (i + 1 < tokens.size) {
                    val x = tokens[i].toFloatOrNull()
                    val y = tokens[i + 1].toFloatOrNull()
                    if (x != null && y != null) points.add(Offset(x, y))
                    i += 2
                }
            }
            token.equals("Z", ignoreCase = true) -> {
                if (points.isNotEmpty()) points.add(points.first())
                i++
            }
            else -> i++
        }
    }
    return points
}

private val NAMED_COLORS = mapOf(
    "black" to Color.Black,
    "white" to Color.White,
    "red" to Color.Red,
    "green" to Color.Green,
    "blue" to Color.Blue,
    "yellow" to Color.Yellow,
    "gray" to Color.Gray,
    "grey" to Color.Gray,
    "orange" to Color(0xFFFFA500),
    "purple" to Color(0xFF800080),
    "cyan" to Color.Cyan,
    "magenta" to Color.Magenta
)

private fun svgColor(raw: String?): Color? {
    if (raw.isNullOrBlank() || raw == "none") return null
    val trimmed = raw.trim()
    if (trimmed.startsWith("#")) return parseHexColor(trimmed)
    return NAMED_COLORS[trimmed.lowercase()]
}

private fun parseHexColor(hex: String): Color? {
    val clean = hex.removePrefix("#")
    return try {
        when (clean.length) {
            3 -> {
                val r = clean[0].toString().repeat(2).toInt(16)
                val g = clean[1].toString().repeat(2).toInt(16)
                val b = clean[2].toString().repeat(2).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f)
            }
            6 -> {
                val value = clean.toLong(16)
                val r = ((value shr 16) and 0xFF) / 255f
                val g = ((value shr 8) and 0xFF) / 255f
                val b = (value and 0xFF) / 255f
                Color(red = r, green = g, blue = b)
            }
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}
