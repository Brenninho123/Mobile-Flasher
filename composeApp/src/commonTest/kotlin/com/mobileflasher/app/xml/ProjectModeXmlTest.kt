package com.mobileflasher.app.xml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.model.hitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectModeXmlTest {

    @Test
    fun keepsTheProjectMode() {
        val art = Project(name = "Sketch", mode = ProjectMode.ART)
        assertEquals(ProjectMode.ART, parseProjectXml(writeProjectXml(art)).mode)
        assertEquals(ProjectMode.ANIMATION, parseProjectXml(writeProjectXml(art.copy(mode = ProjectMode.ANIMATION))).mode)
    }

    @Test
    fun projectsWithoutAModeOpenAsAnimations() {
        val legacy = "<project name=\"Old\" frameRate=\"24\"><layer id=\"layer-1\" name=\"Layer 1\"/></project>"
        assertEquals(ProjectMode.ANIMATION, parseProjectXml(legacy).mode)
    }

    @Test
    fun keepsFilledClosedFreehandShapes() {
        val shape = FreehandShape(
            id = "shape-0",
            points = listOf(Offset(0f, 0f), Offset(40f, 0f), Offset(40f, 40f), Offset(0f, 40f)),
            strokeColor = Color.Transparent,
            strokeWidth = 0f,
            fillColor = Color(0xFF336699),
            closed = true
        )
        val project = Project(
            name = "Filled",
            layers = listOf(Layer("layer-1", "Layer 1", frames = listOf(Frame(0, true, listOf(shape)))))
        )

        val restored = parseProjectXml(writeProjectXml(project)).layers.single().frames.single().shapes.single()

        assertEquals(shape, restored)
    }

    @Test
    fun filledFreehandShapesAreHitInsideTheirOutline() {
        val open = FreehandShape("a", listOf(Offset(0f, 0f), Offset(40f, 0f), Offset(40f, 40f), Offset(0f, 40f)), Color.Black, 2f)
        val filled = open.copy(fillColor = Color.Red, closed = true)

        assertFalse(open.hitTest(Offset(20f, 20f), 1f))
        assertTrue(filled.hitTest(Offset(20f, 20f), 1f))
        assertFalse(filled.hitTest(Offset(80f, 80f), 1f))
    }
}
