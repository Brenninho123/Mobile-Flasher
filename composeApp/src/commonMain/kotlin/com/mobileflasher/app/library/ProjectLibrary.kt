package com.mobileflasher.app.library

import com.mobileflasher.app.mflash.MflashFormat
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.platform.FileStore
import kotlin.random.Random

data class LibraryEntry(
    val id: String,
    val name: String,
    val modifiedMillis: Long,
    val frameCount: Int,
    val layerCount: Int
)

class ProjectLibrary(
    private val files: FileStore,
    private val clock: () -> Long
) {

    fun list(): List<LibraryEntry> {
        val text = files.read(IndexFile)?.decodeToString() ?: return emptyList()
        return text.lineSequence()
            .mapNotNull { parseLine(it) }
            .filter { files.read(projectFile(it.id)) != null }
            .sortedByDescending { it.modifiedMillis }
            .toList()
    }

    fun save(id: String?, project: Project, thumbnailPng: ByteArray?): LibraryEntry? {
        val entryId = id ?: newId()
        val now = clock()
        val bytes = MflashFormat.encode(project, thumbnailPng, now)
        if (!files.write(projectFile(entryId), bytes)) return null
        if (thumbnailPng != null) files.write(thumbnailFile(entryId), thumbnailPng)

        val entry = LibraryEntry(
            id = entryId,
            name = project.name,
            modifiedMillis = now,
            frameCount = project.layers.maxOfOrNull { it.frames.size } ?: 1,
            layerCount = project.layers.size
        )
        val others = readIndex().filter { it.id != entryId }
        writeIndex(others + entry)
        return entry
    }

    fun importBytes(bytes: ByteArray): LibraryEntry? {
        val project = MflashFormat.readProject(bytes)
        val thumbnail = if (MflashFormat.isContainer(bytes)) MflashFormat.decode(bytes).thumbnailPng else null
        return save(null, project, thumbnail)
    }

    fun loadProject(id: String): Project? {
        val bytes = files.read(projectFile(id)) ?: return null
        return try {
            MflashFormat.readProject(bytes)
        } catch (e: Exception) {
            null
        }
    }

    fun loadFileBytes(id: String): ByteArray? = files.read(projectFile(id))

    fun loadThumbnail(id: String): ByteArray? = files.read(thumbnailFile(id))

    fun delete(id: String) {
        files.delete(projectFile(id))
        files.delete(thumbnailFile(id))
        writeIndex(readIndex().filter { it.id != id })
    }

    private fun readIndex(): List<LibraryEntry> {
        val text = files.read(IndexFile)?.decodeToString() ?: return emptyList()
        return text.lineSequence().mapNotNull { parseLine(it) }.toList()
    }

    private fun writeIndex(entries: List<LibraryEntry>) {
        val text = entries.joinToString("\n") { entry ->
            listOf(entry.id, entry.modifiedMillis, entry.frameCount, entry.layerCount, sanitize(entry.name)).joinToString("\t")
        }
        files.write(IndexFile, text.encodeToByteArray())
    }

    private fun parseLine(line: String): LibraryEntry? {
        val parts = line.split('\t', limit = 5)
        if (parts.size < 5) return null
        return LibraryEntry(
            id = parts[0].takeIf { it.isNotBlank() } ?: return null,
            modifiedMillis = parts[1].toLongOrNull() ?: return null,
            frameCount = parts[2].toIntOrNull() ?: 1,
            layerCount = parts[3].toIntOrNull() ?: 1,
            name = parts[4]
        )
    }

    private fun sanitize(name: String): String = name.map { if (it == '\t' || it == '\n' || it == '\r') ' ' else it }.joinToString("")

    private fun newId(): String = "p" + clock().toString(36) + Random.nextInt(1000, 9999).toString(36)

    private fun projectFile(id: String) = "$id.${MflashFormat.Extension}"

    private fun thumbnailFile(id: String) = "$id.png"

    private companion object {
        const val IndexFile = "library.index"
    }
}
