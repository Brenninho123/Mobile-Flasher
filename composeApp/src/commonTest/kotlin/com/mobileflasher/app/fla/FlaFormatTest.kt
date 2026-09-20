package com.mobileflasher.app.fla

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.zip.ZipArchive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlaFormatTest {

    private fun archive(document: String, library: Map<String, String> = emptyMap()): ByteArray {
        val entries = LinkedHashMap<String, ByteArray>()
        entries["DOMDocument.xml"] = document.encodeToByteArray()
        library.forEach { (name, text) -> entries["LIBRARY/$name"] = text.encodeToByteArray() }
        return ZipArchive.write(entries)
    }

    private fun document(layers: String, frameRate: Int = 12) =
        """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<DOMDocument xmlns="http://ns.adobe.com/xfl/2008/" width="550" height="400" frameRate="$frameRate" xflVersion="2.2">
  <timelines><DOMTimeline name="Scene 1"><layers>$layers</layers></DOMTimeline></timelines>
</DOMDocument>"""

    @Test
    fun parsesEdgePathsWithLinesCurvesAndHexNumbers() {
        val paths = FlaEdges.parsePaths("!0 0|200 0[400 0 400 200S3|#190.80 #-A")
        assertEquals(1, paths.size)
        val points = paths.first()
        assertEquals(Offset(0f, 0f), points.first())
        assertEquals(Offset(10f, 0f), points[1])
        assertEquals(Offset(20f, 10f), points[7])
        val last = points.last()
        assertEquals(20.025f, last.x, 0.001f)
        assertEquals(-0.5f, last.y, 0.001f)
    }

    @Test
    fun chainsSegmentsIntoClosedLoops() {
        val chains = FlaEdges.chain(
            listOf(
                listOf(Offset(0f, 0f), Offset(10f, 0f)),
                listOf(Offset(10f, 10f), Offset(0f, 10f), Offset(0f, 0f)),
                listOf(Offset(10f, 0f), Offset(10f, 10f))
            )
        )
        assertEquals(1, chains.size)
        assertTrue(chains.first().closed)
        assertEquals(4, chains.first().points.size)
    }

    @Test
    fun importsFilledAndStrokedShapes() {
        val xml = document(
            """<DOMLayer name="Art" color="#4FFF4F"><frames><DOMFrame index="0" duration="1" keyMode="9728"><elements>
            <DOMShape>
              <fills><FillStyle index="1"><SolidColor color="#FF0000"/></FillStyle></fills>
              <strokes><StrokeStyle index="1"><SolidStroke scaleMode="normal" weight="3"><fill><SolidColor color="#0000FF"/></fill></SolidStroke></StrokeStyle></strokes>
              <edges><Edge fillStyle1="1" strokeStyle="1" edges="!0 0|2000 0|2000 1000|0 1000|0 0"/></edges>
            </DOMShape></elements></DOMFrame></frames></DOMLayer>"""
        )
        val imported = FlaFormat.decode(archive(xml))

        assertEquals(ProjectMode.ART, imported.project.mode)
        assertEquals(12, imported.project.frameRate)
        val shapes = imported.project.layers.single().frames.single().shapes
        assertEquals(2, shapes.size)
        val fill = shapes[0] as FreehandShape
        assertTrue(fill.closed)
        assertEquals(Color(1f, 0f, 0f), fill.fillColor)
        assertEquals(4, fill.points.size)
        assertEquals(Offset(100f, 50f), fill.points[2])
        val stroke = shapes[1] as FreehandShape
        assertEquals(3f, stroke.strokeWidth)
        assertEquals(Color(0f, 0f, 1f), stroke.strokeColor)
        assertEquals(0, imported.skippedElements)
    }

    @Test
    fun importsLayersBottomToTopWithFrameSpans() {
        val rect = """<DOMRectangle objectWidth="40" objectHeight="20" x="5" y="6"><fill><SolidColor color="#00FF00"/></fill></DOMRectangle>"""
        val xml = document(
            """<DOMLayer name="Top"><frames><DOMFrame index="0" duration="2"><elements>$rect</elements></DOMFrame>
                <DOMFrame index="2" duration="1"><elements/></DOMFrame></frames></DOMLayer>
               <DOMLayer name="Bottom" visible="false" locked="true"><frames><DOMFrame index="0"><elements/></DOMFrame></frames></DOMLayer>"""
        )
        val project = FlaFormat.decode(archive(xml)).project

        assertEquals(listOf("Bottom", "Top"), project.layers.map { it.name })
        assertFalse(project.layers[0].isVisible)
        assertTrue(project.layers[0].isLocked)
        val top = project.layers[1]
        assertEquals(3, top.frames.size)
        assertEquals(listOf(true, false, true), top.frames.map { it.isKeyframe })
        assertEquals(top.frames[0].shapes, top.frames[1].shapes)
        assertTrue(top.frames[2].shapes.isEmpty())
        assertEquals(ProjectMode.ANIMATION, project.mode)
        val rectangle = top.frames[0].shapes.single() as RectangleShape
        assertEquals(Offset(5f, 6f), rectangle.topLeft)
        assertEquals(Size(40f, 20f), rectangle.size)
    }

    @Test
    fun appliesSymbolInstanceTransforms() {
        val symbol = """<DOMSymbolItem name="Box" itemID="1"><timeline><DOMTimeline name="Box"><layers>
            <DOMLayer name="Layer 1"><frames><DOMFrame index="0"><elements>
            <DOMRectangle objectWidth="10" objectHeight="10" x="0" y="0"><fill><SolidColor color="#FFFFFF"/></fill></DOMRectangle>
            </elements></DOMFrame></frames></DOMLayer></layers></DOMTimeline></timeline></DOMSymbolItem>"""
        val xml = document(
            """<DOMLayer name="Main"><frames><DOMFrame index="0"><elements>
            <DOMSymbolInstance libraryItemName="Box"><matrix><Matrix a="2" d="2" tx="30" ty="40"/></matrix></DOMSymbolInstance>
            <DOMBitmapInstance libraryItemName="photo.png"/>
            </elements></DOMFrame></frames></DOMLayer>"""
        )
        val imported = FlaFormat.decode(archive(xml, mapOf("Box.xml" to symbol)))
        val rectangle = imported.project.layers.single().frames.single().shapes.single() as RectangleShape

        assertEquals(Offset(30f, 40f), rectangle.topLeft)
        assertEquals(Size(20f, 20f), rectangle.size)
        assertEquals(1, imported.skippedElements)
    }

    @Test
    fun rejectsLegacyBinaryAndNonFlaArchives() {
        val ole = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(), 0, 0)
        assertTrue(FlaFormat.isLegacyBinary(ole))
        assertFailsWith<UnsupportedFlaFormatException> { FlaFormat.decode(ole) }
        assertFailsWith<IllegalArgumentException> { FlaFormat.decode(ZipArchive.write(mapOf("readme.txt" to byteArrayOf(1)))) }
        assertFailsWith<IllegalArgumentException> { FlaFormat.decode(byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun roundTripKeepsGeometryColorsAndTimeline() {
        val red = Color(1f, 0f, 0f)
        val blue = Color(0f, 0f, 1f)
        val first = listOf(
            RectangleShape("a", Offset(10f, 20f), Size(100f, 50f), blue, 2f, red),
            LineShape("b", Offset(0f, 0f), Offset(30f, 40f), blue, 4f)
        )
        val second = first + EllipseShape("c", Offset(50f, 60f), Size(80f, 40f), blue, 1f, null)
        val project = Project(
            name = "Round trip",
            frameRate = 30,
            layers = listOf(
                Layer(
                    id = "layer-1",
                    name = "Ink & <Paint>",
                    frames = listOf(
                        Frame(0, true, first),
                        Frame(1, false, first),
                        Frame(2, true, second)
                    )
                )
            )
        )

        val bytes = FlaFormat.encode(project, 720, 1280)
        assertTrue(FlaFormat.isArchive(bytes))
        val entries = ZipArchive.read(bytes)
        assertContentEquals(listOf("DOMDocument.xml"), entries.keys.toList())
        assertTrue(entries.getValue("DOMDocument.xml").decodeToString().contains("width=\"720\""))

        val imported = FlaFormat.decode(bytes).project
        assertEquals(30, imported.frameRate)
        assertEquals("Ink & <Paint>", imported.layers.single().name)
        val frames = imported.layers.single().frames
        assertEquals(3, frames.size)
        assertEquals(listOf(true, false, true), frames.map { it.isKeyframe })
        assertEquals(frames[0].shapes, frames[1].shapes)

        val filled = frames[0].shapes.filterIsInstance<FreehandShape>().filter { it.fillColor != null }
        assertEquals(1, filled.size)
        assertEquals(red, filled.single().fillColor)
        assertEquals(Offset(10f, 20f), filled.single().points.first())
        assertEquals(Offset(110f, 20f), filled.single().points[1])
        val strokes = frames[0].shapes.filterIsInstance<FreehandShape>().filter { it.fillColor == null }
        assertTrue(strokes.any { it.strokeWidth == 4f && it.points.size == 2 && it.points.last() == Offset(30f, 40f) })
        assertNotNull(frames[2].shapes.filterIsInstance<FreehandShape>().firstOrNull { it.fillColor == null && it.closed })
        assertTrue(frames[2].shapes.size > frames[0].shapes.size)
    }
}
