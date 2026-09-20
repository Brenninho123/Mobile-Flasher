package com.mobileflasher.app.library

import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.platform.FileStore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemoryFiles : FileStore {
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

class ProjectLibraryTest {

    private var now = 1000L
    private val files = MemoryFiles()
    private val library = ProjectLibrary(files) { now }

    private fun project(name: String, frames: Int = 1) = Project(
        name = name,
        layers = listOf(Layer("layer-1", "Layer 1", frames = List(frames) { Frame(it, it == 0) }))
    )

    @Test
    fun startsEmpty() {
        assertTrue(library.list().isEmpty())
    }

    @Test
    fun savesAndListsNewestFirst() {
        library.save(null, project("First"), null)
        now += 500
        library.save(null, project("Second", frames = 4), null)

        val entries = library.list()

        assertEquals(listOf("Second", "First"), entries.map { it.name })
        assertEquals(4, entries.first().frameCount)
    }

    @Test
    fun savingWithAnIdOverwritesTheSameEntry() {
        val entry = library.save(null, project("Draft"), null)!!
        now += 100
        library.save(entry.id, project("Final", frames = 3), null)

        val entries = library.list()

        assertEquals(1, entries.size)
        assertEquals("Final", entries.single().name)
        assertEquals("Final", library.loadProject(entry.id)?.name)
    }

    @Test
    fun storesAndReturnsThumbnail() {
        val entry = library.save(null, project("Art"), byteArrayOf(9, 8, 7))!!
        assertContentEquals(byteArrayOf(9, 8, 7), library.loadThumbnail(entry.id))
    }

    @Test
    fun deleteRemovesEverything() {
        val entry = library.save(null, project("Temp"), byteArrayOf(1))!!
        library.delete(entry.id)

        assertTrue(library.list().isEmpty())
        assertNull(library.loadProject(entry.id))
        assertNull(library.loadThumbnail(entry.id))
    }

    @Test
    fun importsExternalMflashBytesAsNewEntry() {
        val entry = library.save(null, project("Shared"), null)!!
        val bytes = library.loadFileBytes(entry.id)!!
        now += 10

        val imported = library.importBytes(bytes)

        assertNotNull(imported)
        assertNotEquals(entry.id, imported.id)
        assertEquals(2, library.list().size)
    }

    @Test
    fun namesWithTabsAndNewlinesDoNotBreakTheIndex() {
        library.save(null, project("Odd\tName\nHere"), null)
        library.save(null, project("Normal"), null)

        assertEquals(2, library.list().size)
    }

    @Test
    fun skipsIndexEntriesWhoseFileIsMissing() {
        val entry = library.save(null, project("Ghost"), null)!!
        files.files.remove("${entry.id}.mflash")

        assertTrue(library.list().isEmpty())
    }
}
