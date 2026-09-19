package com.mobileflasher.app.model

data class Layer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val frames: List<Frame> = listOf(Frame(index = 0, isKeyframe = true))
)
