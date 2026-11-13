package app.thdev.moru.core.ui.path

/** [path] with a leading [home] shown as `~`, the way a shell prints it; unchanged outside [home] or without one. */
fun homeRelativePath(path: String, home: String): String =
    if (home.isNotEmpty() && path.startsWith(home)) "~" + path.removePrefix(home) else path
