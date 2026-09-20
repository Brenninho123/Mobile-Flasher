package com.mobileflasher.app.platform

import androidx.compose.ui.graphics.ImageBitmap

class VideoClip(val framesPng: List<ByteArray>, val frameRate: Int)

expect fun extractVideoClip(video: ByteArray, maxFrames: Int, maxSide: Int): VideoClip?

expect fun encodeMp4(frames: List<ImageBitmap>, frameRate: Int): ByteArray?
