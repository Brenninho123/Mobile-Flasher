package com.mobileflasher.app.xml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.RectangleShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectXmlTest {

    @Test
    fun roundTripsShapesLayersAndFrames() {
        val project = Project(
            name = "Demo Project",
            frameRate = 12,
            layers = listOf(
                Layer(
                    id = "layer-1",
                    name = "Layer 1",
                    isVisible = true,
                    isLocked = false,
                    frames = listOf(
                        Frame(
                            index = 0,
                            isKeyframe = true,
                            shapes = listOf(
                                RectangleShape(
                                    id = "shape-0",
                                    topLeft = Offset(10f, 20f),
                                    size = Size(100f, 50f),
                                    strokeColor = Color(0xFF112233),
                                    strokeWidth = 3f,
                                    fillColor = Color(0xFFAABBCC)
                                ),
                                LineShape(
                                    id = "shape-1",
                                    start = Offset(0f, 0f),
                                    end = Offset(50f, 60f),
                                    strokeColor = Color.Black,
                                    strokeWidth = 2f
                                ),
                                FreehandShape(
                                    id = "shape-2",
                                    points = listOf(Offset(1f, 1f), Offset(2f, 2f), Offset(3f, 1f)),
                                    strokeColor = Color(0xFFFFB300),
                                    strokeWidth = 4f
                                )
                            )
                        ),
                        Frame(index = 1, isKeyframe = false, shapes = emptyList())
                    )
                )
            )
        )

        val xml = writeProjectXml(project)
        val restored = parseProjectXml(xml)

        assertEquals(project.name, restored.name)
        assertEquals(project.frameRate, restored.frameRate)
        assertEquals(project.layers.size, restored.layers.size)
        assertEquals(project.layers[0].id, restored.layers[0].id)
        assertEquals(project.layers[0].frames.size, restored.layers[0].frames.size)
        assertEquals(project.layers[0].frames[0].shapes, restored.layers[0].frames[0].shapes)
    }

    @Test
    fun rejectsNonProjectXml() {
        var threw = false
        try {
            parseProjectXml("<svg></svg>")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }
}
