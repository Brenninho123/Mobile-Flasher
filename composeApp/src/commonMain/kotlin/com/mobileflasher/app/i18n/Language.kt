package com.mobileflasher.app.i18n

enum class Language(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    PORTUGUESE("pt", "Português"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch");

    companion object {
        val Default = ENGLISH

        fun fromTag(tag: String?): Language? {
            val primary = tag
                ?.trim()
                ?.split('-', '_')
                ?.firstOrNull()
                ?.lowercase()
                ?: return null
            return entries.firstOrNull { it.code == primary }
        }

        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }
    }
}

fun resolveLanguage(preferred: Language?, deviceTags: List<String>): Language =
    preferred ?: deviceTags.firstNotNullOfOrNull { Language.fromTag(it) } ?: Language.Default
