package com.mobileflasher.app.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun ImageBitmap.encodeToPng(): ByteArray

expect fun decodePngToImageBitmap(bytes: ByteArray): ImageBitmap
