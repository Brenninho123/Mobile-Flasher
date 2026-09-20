package com.mobileflasher.app.library

import com.mobileflasher.app.fla.FlaFormat
import com.mobileflasher.app.fla.UnsupportedFlaFormatException
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.platform.FileStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class MapStore : FileStore {
    val files = HashMap<String, ByteArray>()
    override fun read(name: String) = files[name]
    override fun write(name: String, bytes: ByteArray): Boolean {
        files[name] = bytes
        return true
    }
    override fun delete(name: String) {
        files.remove(name)
    }
}

class FlaImportTest {

    private val library = ProjectLibrary(MapStore(), { 1_000L })

    private fun exported(mode: ProjectMode): ByteArray {
        val project = Project(
            name = "Sketch",
            mode = mode,
            layers = listOf(
                Layer(
                    id = "layer-1",
                    name = "Layer 1",
                    frames = listOf(
                        Frame(0, true, listOf(RectangleShape("s", Offset(4f, 4f), Size(20f, 10f), Color.Black, 1f, Color.Red)))
                    )
                )
            )
        )
        return FlaFormat.encode(project, 550, 400)
    }

    @Test
    fun opensAFlaFileIntoTheLibrary() {
        val result = library.importFile(exported(ProjectMode.ART))

        assertNotNull(result)
        val project = library.loadProject(result.entry.id)
        assertNotNull(project)
        assertEquals(ProjectMode.ART, project.mode)
        assertTrue(project.layers.single().frames.single().shapes.any { it is FreehandShape && it.fillColor == Color.Red })
        assertEquals(0, result.skippedElements)
    }

    @Test
    fun projectImportRoutesEachFormat() {
        val fla = ProjectImport.read(exported(ProjectMode.ART))
        assertEquals(1, fla.project.layers.size)

        val saved = library.save(null, fla.project, null)!!
        val mflash = ProjectImport.read(library.loadFileBytes(saved.id)!!)
        assertEquals(fla.project.layers.single().frames.single().shapes.size, mflash.project.layers.single().frames.single().shapes.size)
    }

    @Test
    fun legacyBinaryFlaGetsADedicatedError() {
        val ole = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(), 0, 0, 0)
        assertFailsWith<UnsupportedFlaFormatException> { library.importFile(ole) }
    }
}
