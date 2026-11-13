package app.thdev.moru.app

/** Opens one Project from the menu bar, shown under its name. */
class ProjectCommand(val name: String, val open: () -> Unit)
