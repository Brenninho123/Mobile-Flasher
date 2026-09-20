package com.mobileflasher.app.library

import com.mobileflasher.app.fla.FlaFormat
import com.mobileflasher.app.mflash.MflashFormat
import com.mobileflasher.app.model.Project

class ImportedProject(
    val project: Project,
    val thumbnailPng: ByteArray?,
    val skippedElements: Int
)

object ProjectImport {

    fun read(bytes: ByteArray): ImportedProject {
        if (MflashFormat.isContainer(bytes)) {
            val document = MflashFormat.decode(bytes)
            return ImportedProject(document.project, document.thumbnailPng, 0)
        }
        if (FlaFormat.isLegacyBinary(bytes) || FlaFormat.isArchive(bytes)) {
            val imported = FlaFormat.decode(bytes)
            return ImportedProject(imported.project, null, imported.skippedElements)
        }
        return ImportedProject(MflashFormat.readProject(bytes), null, 0)
    }
}
