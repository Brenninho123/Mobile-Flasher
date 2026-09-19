package com.mobileflasher.app.xml

data class XmlNode(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlNode>,
    val text: String
)

fun parseXml(source: String): XmlNode {
    val state = XmlParserState(source)
    state.skipProlog()
    return state.parseElement()
}

fun escapeXml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun unescapeXml(text: String): String {
    return text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}

private class XmlParserState(private val source: String) {
    private var pos = 0

    fun skipProlog() {
        skipWhitespaceAndComments()
        if (source.startsWith("<?", pos)) {
            val end = source.indexOf("?>", pos)
            pos = if (end >= 0) end + 2 else source.length
        }
        skipWhitespaceAndComments()
        if (source.startsWith("<!DOCTYPE", pos, ignoreCase = true)) {
            val end = source.indexOf(">", pos)
            pos = if (end >= 0) end + 1 else source.length
        }
        skipWhitespaceAndComments()
    }

    fun parseElement(): XmlNode {
        skipWhitespaceAndComments()
        require(pos < source.length && source[pos] == '<') { "Expected '<' at position $pos" }
        pos++
        val name = readName()
        val attributes = LinkedHashMap<String, String>()
        while (true) {
            skipInlineWhitespace()
            if (source.startsWith("/>", pos)) {
                pos += 2
                return XmlNode(name, attributes, emptyList(), "")
            }
            if (source[pos] == '>') {
                pos++
                break
            }
            val attrName = readName()
            skipInlineWhitespace()
            require(source[pos] == '=') { "Expected '=' after attribute name '$attrName'" }
            pos++
            skipInlineWhitespace()
            val quote = source[pos]
            require(quote == '"' || quote == '\'') { "Expected quote for attribute value" }
            pos++
            val valueStart = pos
            while (source[pos] != quote) pos++
            val rawValue = source.substring(valueStart, pos)
            pos++
            attributes[attrName] = unescapeXml(rawValue)
        }

        val children = mutableListOf<XmlNode>()
        val textBuilder = StringBuilder()
        while (true) {
            if (source.startsWith("</", pos)) {
                pos += 2
                val closeName = readName()
                require(closeName == name) { "Mismatched closing tag: expected </$name>, got </$closeName>" }
                skipInlineWhitespace()
                require(source[pos] == '>') { "Expected '>' to close tag $name" }
                pos++
                break
            } else if (source.startsWith("<!--", pos)) {
                val end = source.indexOf("-->", pos)
                pos = if (end >= 0) end + 3 else source.length
            } else if (source[pos] == '<') {
                children.add(parseElement())
            } else {
                val textStart = pos
                while (pos < source.length && source[pos] != '<') pos++
                textBuilder.append(unescapeXml(source.substring(textStart, pos)))
            }
        }
        return XmlNode(name, attributes, children, textBuilder.toString().trim())
    }

    private fun readName(): String {
        val start = pos
        while (pos < source.length && (source[pos].isLetterOrDigit() || source[pos] in "_-:.")) pos++
        return source.substring(start, pos)
    }

    private fun skipInlineWhitespace() {
        while (pos < source.length && source[pos].isWhitespace()) pos++
    }

    private fun skipWhitespaceAndComments() {
        while (pos < source.length) {
            when {
                source[pos].isWhitespace() -> pos++
                source.startsWith("<!--", pos) -> {
                    val end = source.indexOf("-->", pos)
                    pos = if (end >= 0) end + 3 else source.length
                }
                else -> return
            }
        }
    }
}
