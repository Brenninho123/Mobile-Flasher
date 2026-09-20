package com.mobileflasher.app.model

enum class ProjectMode(val code: String) {
    ART("art"),
    ANIMATION("animation");

    companion object {
        fun fromCode(code: String?): ProjectMode = entries.firstOrNull { it.code == code } ?: ANIMATION
    }
}

data class Project(
    val name: String,
    val frameRate: Int = 24,
    val layers: List<Layer> = listOf(Layer(id = "layer-1", name = "Layer 1")),
    val mode: ProjectMode = ProjectMode.ANIMATION
)
