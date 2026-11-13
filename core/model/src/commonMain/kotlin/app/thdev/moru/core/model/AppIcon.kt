package app.thdev.moru.core.model

/**
 * The pictures the user can give the app in the Dock while it runs (Settings › Appearance). The app bundle keeps its
 * own icon, so Finder and Launchpad always show [Moru]. Stored by [id], so a later build's choice reads as [Moru] here.
 */
enum class AppIcon(val id: String) {
    /** The taegeuk in cream and charcoal. */
    Moru("moru"),

    /** The cyan taegeuk with a prompt, from before the rename. */
    Classic("classic"),

    /** An anvil, 모루. */
    Anvil("anvil"),

    /** A rounded m with an amber dot. */
    M("m"),

    /** Mo the cat. */
    Mo("mo"),
    ;

    companion object {
        fun fromId(id: String?): AppIcon = entries.firstOrNull { it.id == id } ?: Moru
    }
}
