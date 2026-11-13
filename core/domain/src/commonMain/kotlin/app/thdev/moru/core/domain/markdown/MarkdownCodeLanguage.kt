package app.thdev.moru.core.domain.markdown

import app.thdev.moru.core.domain.code.CodeLanguage

/** Exact existing fenced-code aliases; the shared scanner owns highlighting. */
object MarkdownCodeLanguage {
    fun of(name: String): CodeLanguage = when (val fence=name.trim().lowercase()) {
        "kotlin" -> CodeLanguage.Kotlin
        "javascript", "typescript" -> CodeLanguage.Script
        "python" -> CodeLanguage.Python
        "golang" -> CodeLanguage.Go
        "rust" -> CodeLanguage.Rust
        "shell", "console", "shellscript" -> CodeLanguage.Shell
        "objc", "objective-c" -> CodeLanguage.Swift
        else -> CodeLanguage.of("code.$fence")
    }
}
