package app.thdev.moru.feature.workbench

/**
 * Sidebar group a Task belongs to. Declaration order is the display order:
 * what needs the user comes first, finished work last.
 */
enum class TaskGroup {
    NeedsAttention,
    Running,
    Review,
    Done,
}
