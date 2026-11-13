package app.thdev.moru.core.data

import app.thdev.moru.core.domain.WorkbenchLayoutRepository
import app.thdev.moru.core.model.WorkbenchLayout

/** A layout store that lives only as long as the process; for tests and for runs that must not touch the user's files. */
class InMemoryWorkbenchLayoutRepository(private var layout: WorkbenchLayout = WorkbenchLayout()) : WorkbenchLayoutRepository {
    override fun loadLayout(): WorkbenchLayout = layout

    override fun saveLayout(layout: WorkbenchLayout) {
        this.layout = layout
    }
}
