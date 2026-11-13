package app.thdev.moru.core.designsystem.material

/** A composition releases its lease, while another host can keep the same window material alive. */
internal class MacBackdropLease(
    val controller: MacBackdropController,
    private val release: () -> Unit,
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            release()
        }
    }
}
