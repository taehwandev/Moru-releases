package app.thdev.moru.core.domain

import app.thdev.moru.core.model.WorkbenchLayout

/** Keeps the [WorkbenchLayout] between launches. A missing or unreadable store reads as an empty layout. */
interface WorkbenchLayoutRepository {
    fun loadLayout(): WorkbenchLayout

    fun saveLayout(layout: WorkbenchLayout)
}
