package app.thdev.moru.feature.workbench

/** Preserve the Workbench call site while the ranking rule lives in portable core. */
fun quickOpenMatches(query: String, paths: List<String>): List<String> =
    app.thdev.moru.core.domain.files.quickOpenMatches(query, paths)
