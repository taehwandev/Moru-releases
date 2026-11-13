package app.thdev.moru.core.designsystem.component

/**
 * The status vocabulary of the workbench. Every kind differs in shape as well as colour, so a state never
 * depends on colour alone: a spinning arc is working, a solid diamond is waiting for the user, a small dot
 * is unread, a check is done, a cross is failed and an empty ring is idle.
 */
enum class MoruStatusGlyphKind {
    Working,
    NeedsYou,
    Unread,
    Done,
    Error,
    Idle,
}
