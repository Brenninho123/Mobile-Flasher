package com.mobileflasher.app.mflash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.xml.writeProjectXml
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MflashFormatTest {

    private val project = Project(
        name = "Sketchbook",
        frameRate = 12,
        layers = listOf(
            Layer(
                id = "layer-1",
                name = "Ink",
                frames = listOf(
                    Frame(
                        index = 0,
                        isKeyframe = true,
                        shapes = listOf(
                            RectangleShape("shape-0", Offset(4f, 8f), Size(40f, 20f), Color(0xFF102030), 2f, Color(0xFFAABBCC))
                        )
                    ),
                    Frame(index = 1, isKeyframe = false)
                )
            )
        )
    )

    @Test
    fun roundTripsProjectThumbnailAndTimestamp() {
        val thumbnail = byteArrayOf(1, 2, 3, 4, 5)
        val bytes = MflashFormat.encode(project, thumbnail, savedAtMillis = 1234567890123L)

        val document = MflashFormat.decode(bytes)

        assertEquals(project, document.project)
        assertContentEquals(thumbnail, document.thumbnailPng)
        assertEquals(1234567890123L, document.savedAtMillis)
    }

    @Test
    fun thumbnailIsOptional() {
        val document = MflashFormat.decode(MflashFormat.encode(project, null, 1L))
        assertNull(document.thumbnailPng)
    }

    @Test
    fun startsWithMagicHeader() {
        val bytes = MflashFormat.encode(project, null, 0L)
        assertTrue(MflashFormat.isContainer(bytes))
        assertEquals("MFLASH", bytes.copyOfRange(0, 6).decodeToString())
        assertFalse(MflashFormat.isContainer("<?xml".encodeToByteArray()))
    }

    @Test
    fun detectsCorruption() {
        val bytes = MflashFormat.encode(project, null, 0L)
        bytes[bytes.size / 2] = (bytes[bytes.size / 2] + 1).toByte()
        assertFailsWith<IllegalArgumentException> { MflashFormat.decode(bytes) }
    }

    @Test
    fun detectsTruncation() {
        val bytes = MflashFormat.encode(project, null, 0L)
        assertFailsWith<IllegalArgumentException> { MflashFormat.decode(bytes.copyOf(bytes.size - 10)) }
        assertFailsWith<IllegalArgumentException> { MflashFormat.decode(bytes.copyOf(8)) }
    }

    @Test
    fun rejectsNewerFormatVersions() {
        val bytes = MflashFormat.encode(project, null, 0L)
        bytes[6] = 99
        assertFailsWith<IllegalArgumentException> { MflashFormat.decode(bytes) }
    }

    @Test
    fun readProjectAcceptsLegacyXmlFiles() {
        val legacy = writeProjectXml(project).encodeToByteArray()
        assertEquals(project, MflashFormat.readProject(legacy))
        assertEquals(project, MflashFormat.readProject(MflashFormat.encode(project, null, 0L)))
    }

    @Test
    fun readProjectRejectsUnrelatedFiles() {
        assertFailsWith<IllegalArgumentException> { MflashFormat.readProject("hello".encodeToByteArray()) }
    }
}
