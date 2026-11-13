package app.thdev.moru.core.model.git.diff

/** A contiguous changed region. [header] is git's `@@ -a,b +c,d @@ section` line, kept as printed. */
data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>,
)
