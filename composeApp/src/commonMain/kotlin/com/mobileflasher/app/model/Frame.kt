package com.mobileflasher.app.model

data class Frame(
    val index: Int,
    val isKeyframe: Boolean,
    val shapes: List<VectorShape> = emptyList()
)
