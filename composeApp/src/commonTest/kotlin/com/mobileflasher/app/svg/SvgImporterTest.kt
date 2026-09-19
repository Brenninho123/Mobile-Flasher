package com.mobileflasher.app.svg

import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.RectangleShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgImporterTest {

    @Test
    fun importsBasicShapesWithAttributes() {
        val svg = """
            <svg width="200" height="200">
                <rect x="10" y="20" width="100" height="50" fill="#FF0000" stroke="#000000" stroke-width="2" />
                <circle cx="50" cy="50" r="25" fill="none" stroke="blue" />
                <line x1="0" y1="0" x2="10" y2="10" stroke="#00FF00" />
                <polyline points="0,0 10,10 20,0" stroke="black" />
            </svg>
        """.trimIndent()

        var counter = 0
        val shapes = importSvg(svg) { "svg-shape-${counter++}" }

        assertEquals(4, shapes.size)
        assertTrue(shapes[0] is RectangleShape)
        assertTrue(shapes[1] is EllipseShape)
        assertTrue(shapes[2] is LineShape)
        assertTrue(shapes[3] is FreehandShape)

        val rect = shapes[0] as RectangleShape
        assertEquals(10f, rect.topLeft.x)
        assertEquals(20f, rect.topLeft.y)
        assertEquals(100f, rect.size.width)
        assertEquals(50f, rect.size.height)

        val circle = shapes[1] as EllipseShape
        assertEquals(25f, circle.topLeft.x)
        assertEquals(25f, circle.topLeft.y)
        assertEquals(50f, circle.size.width)
    }

    @Test
    fun importsSimpleAbsolutePathCommands() {
        val svg = """<svg><path d="M10 10 L50 10 L50 50 Z" stroke="black" /></svg>"""
        var counter = 0
        val shapes = importSvg(svg) { "svg-shape-${counter++}" }
        assertEquals(1, shapes.size)
        val freehand = shapes[0] as FreehandShape
        assertEquals(4, freehand.points.size)
        assertEquals(freehand.points.first(), freehand.points.last())
    }

    @Test
    fun assignsSequentialIds() {
        val svg = """<svg><rect x="0" y="0" width="1" height="1" /><rect x="1" y="1" width="1" height="1" /></svg>"""
        var counter = 0
        val shapes = importSvg(svg) { "id-${counter++}" }
        assertEquals(listOf("id-0", "id-1"), shapes.map { it.id })
    }
}
