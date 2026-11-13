package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitWorkingFile
import app.thdev.moru.core.domain.git.GitStatusRecords
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object GitStatusParser {
    fun records(bytes: ByteArray): List<String> {
        if (bytes.isEmpty()) return emptyList()
        require(bytes.last() == 0.toByte()) { "Git returned incomplete discovery output." }
        return decode(bytes).dropLast(1).split('\u0000')
    }

    fun decode(bytes: ByteArray): String = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes)).toString()

    fun parse(bytes: ByteArray): List<GitWorkingFile> = GitStatusRecords.parse(records(bytes))

    fun validPath(value: String): Boolean = GitStatusRecords.validPath(value)
}
