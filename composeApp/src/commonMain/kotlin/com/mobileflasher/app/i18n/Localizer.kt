package com.mobileflasher.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

class Localizer(val language: Language) {

    fun text(key: StringKey, vararg args: Any?): String = text(key, args.asList())

    fun text(key: StringKey, args: List<Any?>): String {
        val template = catalogFor(language)[key] ?: EnglishCatalog.getValue(key)
        return if (args.isEmpty()) template else fill(template, args)
    }

    fun text(message: Message): String = text(message.key, message.args)

    private fun fill(template: String, args: List<Any?>): String {
        val out = StringBuilder(template.length + 16)
        var index = 0
        while (index < template.length) {
            val ch = template[index]
            if (ch == '{') {
                val close = template.indexOf('}', index)
                val position = if (close > index + 1) template.substring(index + 1, close).toIntOrNull() else null
                if (position != null && position in args.indices) {
                    out.append(args[position])
                    index = close + 1
                    continue
                }
            }
            out.append(ch)
            index++
        }
        return out.toString()
    }
}

data class Message(val key: StringKey, val args: List<Any?> = emptyList())

fun message(key: StringKey, vararg args: Any?): Message = Message(key, args.asList())

val LocalLocalizer = staticCompositionLocalOf { Localizer(Language.Default) }

@Composable
fun tr(key: StringKey, vararg args: Any?): String = LocalLocalizer.current.text(key, args.asList())

@Composable
fun tr(message: Message): String = LocalLocalizer.current.text(message)

internal fun catalogFor(language: Language): Map<StringKey, String> = when (language) {
    Language.ENGLISH -> EnglishCatalog
    Language.PORTUGUESE -> PortugueseCatalog
    Language.SPANISH -> SpanishCatalog
    Language.FRENCH -> FrenchCatalog
    Language.GERMAN -> GermanCatalog
}
