package com.mobileflasher.app.model

data class Project(
    val name: String,
    val frameRate: Int = 24,
    val layers: List<Layer> = listOf(Layer(id = "layer-1", name = "Layer 1"))
)
