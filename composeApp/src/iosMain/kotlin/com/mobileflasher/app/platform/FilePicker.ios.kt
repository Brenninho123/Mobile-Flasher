package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeGIF
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

private var retainedPickerDelegate: NSObject? = null

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), bytes, length)
    }
    return out
}

@OptIn(ExperimentalForeignApi::class)
private fun readBytesFromFile(path: String): ByteArray? {
    val file = fopen(path, "rb") ?: return null
    fseek(file, 0, SEEK_END)
    val size = ftell(file)
    fseek(file, 0, SEEK_SET)
    if (size <= 0) {
        fclose(file)
        return ByteArray(0)
    }
    val buffer = ByteArray(size.toInt())
    buffer.usePinned { pinned ->
        fread(pinned.addressOf(0), 1u, size.toULong(), file)
    }
    fclose(file)
    return buffer
}

private fun currentRootViewController() = UIApplication.sharedApplication.keyWindow?.rootViewController

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onPicked: (ByteArray) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]) as? UIImage
        val data = image?.let { UIImagePNGRepresentation(it) }
        if (data != null) onPicked(data.toByteArray())
        retainedPickerDelegate = null
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        retainedPickerDelegate = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DocumentPickerDelegate(
    private val onPicked: (ByteArray) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url != null) {
            val accessing = url.startAccessingSecurityScopedResource()
            val data = url.path?.let { readBytesFromFile(it) }
            if (accessing) url.stopAccessingSecurityScopedResource()
            if (data != null) onPicked(data)
        }
        retainedPickerDelegate = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        retainedPickerDelegate = null
    }
}

@Composable
actual fun rememberFilePicker(mode: FilePickerMode, onFilePicked: (ByteArray) -> Unit): () -> Unit {
    return {
        when (mode) {
            FilePickerMode.IMAGE -> {
                val picker = UIImagePickerController()
                picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                val delegate = ImagePickerDelegate(onFilePicked)
                retainedPickerDelegate = delegate
                picker.delegate = delegate
                currentRootViewController()?.presentViewController(picker, animated = true, completion = null)
            }
            FilePickerMode.DOCUMENT -> {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData))
                val delegate = DocumentPickerDelegate(onFilePicked)
                retainedPickerDelegate = delegate
                picker.delegate = delegate
                currentRootViewController()?.presentViewController(picker, animated = true, completion = null)
            }
            FilePickerMode.MEDIA -> {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeGIF, UTTypeMovie))
                val delegate = DocumentPickerDelegate(onFilePicked)
                retainedPickerDelegate = delegate
                picker.delegate = delegate
                currentRootViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
