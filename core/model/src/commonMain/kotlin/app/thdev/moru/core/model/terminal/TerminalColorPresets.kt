package app.thdev.moru.core.model.terminal

/** Well-known terminal color schemes, with their published colors. */
object TerminalColorPresets {
    val all: List<TerminalColorPreset> = listOf(
        preset(
            "Solarized Dark", "#839496", "#002b36", "#073642",
            "#073642 #dc322f #859900 #b58900 #268bd2 #d33682 #2aa198 #eee8d5 " +
                "#002b36 #cb4b16 #586e75 #657b83 #839496 #6c71c4 #93a1a1 #fdf6e3",
        ),
        preset(
            "Solarized Light", "#657b83", "#fdf6e3", "#eee8d5",
            "#073642 #dc322f #859900 #b58900 #268bd2 #d33682 #2aa198 #eee8d5 " +
                "#002b36 #cb4b16 #586e75 #657b83 #839496 #6c71c4 #93a1a1 #fdf6e3",
        ),
        preset(
            "Dracula", "#f8f8f2", "#282a36", "#44475a",
            "#21222c #ff5555 #50fa7b #f1fa8c #bd93f9 #ff79c6 #8be9fd #f8f8f2 " +
                "#6272a4 #ff6e6e #69ff94 #ffffa5 #d6acff #ff92df #a4ffff #ffffff",
        ),
        preset(
            "One Dark", "#abb2bf", "#282c34", "#3e4451",
            "#282c34 #e06c75 #98c379 #e5c07b #61afef #c678dd #56b6c2 #abb2bf " +
                "#5c6370 #e06c75 #98c379 #e5c07b #61afef #c678dd #56b6c2 #ffffff",
        ),
        preset(
            "Gruvbox Dark", "#ebdbb2", "#282828", "#504945",
            "#282828 #cc241d #98971a #d79921 #458588 #b16286 #689d6a #a89984 " +
                "#928374 #fb4934 #b8bb26 #fabd2f #83a598 #d3869b #8ec07c #ebdbb2",
        ),
        preset(
            "Nord", "#d8dee9", "#2e3440", "#434c5e",
            "#3b4252 #bf616a #a3be8c #ebcb8b #81a1c1 #b48ead #88c0d0 #e5e9f0 " +
                "#4c566a #bf616a #a3be8c #ebcb8b #81a1c1 #b48ead #8fbcbb #eceff4",
        ),
        preset(
            "GitHub Light", "#24292f", "#ffffff", "#c8e1ff",
            "#24292f #cf222e #116329 #4d2d00 #0969da #8250df #1b7c83 #6e7781 " +
                "#57606a #a40e26 #1a7f37 #633c01 #218bff #a475f9 #3192aa #8c959f",
        ),
    )

    /** The preset whose colors [scheme] is exactly, if any. */
    fun matching(scheme: TerminalColorScheme?): TerminalColorPreset? = all.firstOrNull { it.scheme == scheme }

    private fun preset(name: String, foreground: String, background: String, selection: String, ansi: String) =
        TerminalColorPreset(name, TerminalColorScheme(foreground, background, selection, ansi = ansi.split(' ')))
}
