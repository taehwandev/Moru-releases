package app.thdev.moru.core.system

/** Reaches nobody; the default where the platform has no way to. */
object NoAttentionNotifier : AttentionNotifier {
    override fun show(title: String, body: String) = Unit

    override fun badge(count: Int) = Unit
}
