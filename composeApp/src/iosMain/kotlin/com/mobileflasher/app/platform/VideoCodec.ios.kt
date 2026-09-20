package com.mobileflasher.app.platform

import androidx.compose.ui.graphics.ImageBitmap

actual fun extractVideoClip(video: ByteArray, maxFrames: Int, maxSide: Int): VideoClip? = null

actual fun encodeMp4(frames: List<ImageBitmap>, frameRate: Int): ByteArray? = null
