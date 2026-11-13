package app.thdev.moru.core.model.code

/** Opaque sRGB colors shared by native and Compose adapters, independent of either UI toolkit. */
data class CodeSyntaxPalette(
    val keyword: String,
    val string: String,
    val number: String,
    val comment: String,
    val annotation: String,
    val type: String,
    val function: String,
) {
    companion object {
        val Light = CodeSyntaxPalette("#8A2BB8", "#1F7A3A", "#1558B0", "#7A7F87", "#9A6200", "#00737A", "#2A55C9")
        val Dark = CodeSyntaxPalette("#CF8EF4", "#9BD08A", "#79B8FF", "#8B929C", "#E5C07B", "#6FD3D8", "#82AAFF")
    }
}
