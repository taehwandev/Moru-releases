package app.thdev.moru.core.model.git.diff

/** What a diff line does to the file. A marker alone (+, -) would be color-only, so the UI draws it too. */
enum class DiffLineKind {
    Context,
    Added,
    Removed,
}
