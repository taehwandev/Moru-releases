package app.thdev.moru.core.domain.git

/** Git's quoted path syntax, including octal UTF-8 bytes. No filesystem normalization is performed. */
internal object GitPathText {
    data class Token(val value: String, val end: Int)

    fun decode(raw: String): String? = if (raw.startsWith('"')) {
        token(raw, 0)?.takeIf { it.end == raw.length }?.value
    } else raw.takeIf { it.isNotEmpty() && '\u0000' !in it }

    fun token(text: String, start: Int): Token? {
        if (start >= text.length || text[start] != '"') return null
        val bytes = mutableListOf<Byte>()
        var index = start + 1
        var plainStart = index
        fun appendPlain(end: Int) { bytes.addAll(text.substring(plainStart, end).encodeToByteArray().toList()) }
        while (index < text.length) {
            when (text[index]) {
                '"' -> {
                    appendPlain(index)
                    val value = runCatching { bytes.toByteArray().decodeToString(throwOnInvalidSequence = true) }.getOrNull()
                    return value?.takeIf { it.isNotEmpty() && '\u0000' !in it }?.let { Token(it, index + 1) }
                }
                '\\' -> {
                    appendPlain(index)
                    index++
                    if (index == text.length) return null
                    val escaped = text[index]
                    val value = when (escaped) {
                        'a' -> 7; 'b' -> 8; 't' -> 9; 'n' -> 10; 'v' -> 11; 'f' -> 12; 'r' -> 13
                        '\\' -> 92; '"' -> 34
                        in '0'..'7' -> {
                            var octal = escaped - '0'
                            var digits = 1
                            while (digits < 3 && index + 1 < text.length && text[index + 1] in '0'..'7') {
                                index++; digits++; octal = octal * 8 + (text[index] - '0')
                            }
                            if (octal > 255) return null
                            octal
                        }
                        else -> return null
                    }
                    bytes += value.toByte()
                    index++; plainStart = index
                }
                else -> index++
            }
        }
        return null
    }
}
