package app.thdev.moru.app

/** How the macOS application is drawn, by AppKit's own appearance name; [System] leaves the choice to the computer. */
internal enum class MacAppearanceLook(val appKitName: String?) {
    Dark("NSAppearanceNameDarkAqua"),
    Light("NSAppearanceNameAqua"),
    System(null),
}
