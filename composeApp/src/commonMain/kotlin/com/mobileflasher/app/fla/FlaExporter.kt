package com.mobileflasher.app.fla

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.xml.escapeXml
import com.mobileflasher.app.zip.ZipArchive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal object FlaExporter {

    private const val EllipseSegments = 8

    fun export(project: Project, width: Int, height: Int): ByteArray {
        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
        xml.append("<DOMDocument xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://ns.adobe.com/xfl/2008/\" ")
        xml.append("backgroundColor=\"#FFFFFF\" width=\"${width.coerceAtLeast(1)}\" height=\"${height.coerceAtLeast(1)}\" ")
        xml.append("frameRate=\"${project.frameRate}\" currentTimeline=\"1\" xflVersion=\"2.2\" creatorInfo=\"Mobile Flasher\">\n")
        xml.append("  <timelines>\n    <DOMTimeline name=\"Scene 1\">\n      <layers>\n")
        for (layer in project.layers.asReversed()) writeLayer(xml, layer)
        xml.append("      </layers>\n    </DOMTimeline>\n  </timelines>\n</DOMDocument>\n")
        return ZipArchive.write(linkedMapOf("DOMDocument.xml" to xml.toString().encodeToByteArray()))
    }

    private fun writeLayer(xml: StringBuilder, layer: Layer) {
        xml.append("        <DOMLayer name=\"${escapeXml(layer.name)}\"")
        if (!layer.isVisible) xml.append(" visible=\"false\"")
        if (layer.isLocked) xml.append(" locked=\"true\"")
        xml.append(">\n          <frames>\n")
        var index = 0
        while (index < layer.frames.size) {
            val start = layer.frames[index]
            var end = index + 1
            while (end < layer.frames.size && !layer.frames[end].isKeyframe && layer.frames[end].shapes == start.shapes) end++
            writeFrame(xml, start, index, end - index)
            index = end
        }
        xml.append("          </frames>\n        </DOMLayer>\n")
    }

    private fun writeFrame(xml: StringBuilder, frame: Frame, index: Int, duration: Int) {
        xml.append("            <DOMFrame index=\"$index\" duration=\"$duration\" keyMode=\"9728\">\n")
        val shapes = frame.shapes.mapNotNull { shapeXml(it) }
        if (shapes.isEmpty()) {
            xml.append("              <elements/>\n")
        } else {
            xml.append("              <elements>\n")
            shapes.forEach { xml.append(it) }
            xml.append("              </elements>\n")
        }
        xml.append("            </DOMFrame>\n")
    }

    private fun shapeXml(shape: VectorShape): String? = when (shape) {
        is RectangleShape -> {
            val a = shape.topLeft
            val b = Offset(a.x + shape.size.width, a.y + shape.size.height)
            polygon(listOf(a, Offset(b.x, a.y), b, Offset(a.x, b.y)), shape.fillColor, shape.strokeColor, shape.strokeWidth)
        }
        is EllipseShape -> ellipse(shape)
        is LineShape -> outline(listOf(shape.start, shape.end), shape.strokeColor, shape.strokeWidth)
        is FreehandShape -> when {
            shape.points.size < 2 -> null
            shape.closed -> polygon(shape.points, shape.fillColor, shape.strokeColor, shape.strokeWidth)
            else -> outline(shape.points, shape.strokeColor, shape.strokeWidth)
        }
        else -> null
    }

    private fun polygon(points: List<Offset>, fill: Color?, stroke: Color, strokeWidth: Float): String {
        val hasStroke = stroke.alpha > 0f && strokeWidth > 0f
        val ring = points + points.first()
        val clockwise = signedArea(points) >= 0f
        val attributes = StringBuilder()
        if (fill != null) attributes.append(if (clockwise) " fillStyle1=\"1\"" else " fillStyle0=\"1\"")
        if (hasStroke) attributes.append(" strokeStyle=\"1\"")
        return domShape(fill, if (hasStroke) stroke to strokeWidth else null, "<Edge$attributes edges=\"${FlaEdges.format(ring)}\"/>")
    }

    private fun outline(points: List<Offset>, stroke: Color, strokeWidth: Float): String =
        domShape(null, stroke to strokeWidth, "<Edge strokeStyle=\"1\" edges=\"${FlaEdges.format(points)}\"/>")

    private fun ellipse(shape: EllipseShape): String {
        val cx = shape.topLeft.x + shape.size.width / 2f
        val cy = shape.topLeft.y + shape.size.height / 2f
        val rx = shape.size.width / 2f
        val ry = shape.size.height / 2f
        val step = 2.0 * PI / EllipseSegments
        val controlScale = 1.0 / cos(step / 2.0)
        fun onCurve(angle: Double) = Offset(cx + rx * cos(angle).toFloat(), cy + ry * sin(angle).toFloat())
        fun control(angle: Double) = Offset(
            cx + rx * (controlScale * cos(angle)).toFloat(),
            cy + ry * (controlScale * sin(angle)).toFloat()
        )
        val path = StringBuilder()
        val start = onCurve(0.0)
        path.append("!${FlaEdges.twips(start.x)} ${FlaEdges.twips(start.y)}")
        for (segment in 0 until EllipseSegments) {
            val c = control(step * segment + step / 2.0)
            val end = if (segment == EllipseSegments - 1) start else onCurve(step * (segment + 1))
            path.append("[${FlaEdges.twips(c.x)} ${FlaEdges.twips(c.y)} ${FlaEdges.twips(end.x)} ${FlaEdges.twips(end.y)}")
        }
        val hasStroke = shape.strokeColor.alpha > 0f && shape.strokeWidth > 0f
        val attributes = StringBuilder()
        if (shape.fillColor != null) attributes.append(" fillStyle1=\"1\"")
        if (hasStroke) attributes.append(" strokeStyle=\"1\"")
        return domShape(
            shape.fillColor,
            if (hasStroke) shape.strokeColor to shape.strokeWidth else null,
            "<Edge$attributes edges=\"$path\"/>"
        )
    }

    private fun domShape(fill: Color?, stroke: Pair<Color, Float>?, edge: String): String {
        val out = StringBuilder()
        out.append("                <DOMShape>\n")
        if (fill != null) {
            out.append("                  <fills><FillStyle index=\"1\">${solid(fill)}</FillStyle></fills>\n")
        }
        if (stroke != null) {
            out.append(
                "                  <strokes><StrokeStyle index=\"1\"><SolidStroke scaleMode=\"normal\" weight=\"${number(stroke.second)}\">" +
                    "<fill>${solid(stroke.first)}</fill></SolidStroke></StrokeStyle></strokes>\n"
            )
        }
        out.append("                  <edges>$edge</edges>\n")
        out.append("                </DOMShape>\n")
        return out.toString()
    }

    private fun solid(color: Color): String {
        val rgb = ((color.red * 255f).roundToInt() shl 16) or ((color.green * 255f).roundToInt() shl 8) or (color.blue * 255f).roundToInt()
        val hex = "#" + rgb.toString(16).padStart(6, '0').uppercase()
        return if (color.alpha >= 0.999f) {
            "<SolidColor color=\"$hex\"/>"
        } else {
            "<SolidColor color=\"$hex\" alpha=\"${number(color.alpha)}\"/>"
        }
    }

    private fun number(value: Float): String {
        val rounded = (value * 100f).roundToInt() / 100f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
    }

    private fun signedArea(points: List<Offset>): Float {
        var sum = 0f
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += a.x * b.y - b.x * a.y
        }
        return sum
    }
}
