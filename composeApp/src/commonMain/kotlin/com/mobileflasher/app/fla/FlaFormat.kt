package com.mobileflasher.app.fla

import com.mobileflasher.app.model.Project
import com.mobileflasher.app.zip.ZipArchive

class FlaImport(val project: Project, val skippedElements: Int)

class UnsupportedFlaFormatException(message: String) : IllegalArgumentException(message)

object FlaFormat {

    const val Extension = "fla"
    const val MimeType = "application/octet-stream"

    private val CompoundFileSignature = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()
    )

    fun isLegacyBinary(bytes: ByteArray): Boolean =
        bytes.size >= CompoundFileSignature.size && CompoundFileSignature.indices.all { bytes[it] == CompoundFileSignature[it] }

    fun isArchive(bytes: ByteArray): Boolean = ZipArchive.looksLikeZip(bytes)

    fun decode(bytes: ByteArray): FlaImport {
        if (isLegacyBinary(bytes)) {
            throw UnsupportedFlaFormatException("This .fla uses the legacy binary format")
        }
        return FlaImporter(ZipArchive.read(bytes)).run()
    }

    fun encode(project: Project, width: Int, height: Int): ByteArray = FlaExporter.export(project, width, height)
}
