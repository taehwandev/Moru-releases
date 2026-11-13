package app.thdev.moru.core.system

/** How the app reaches the user outside its own window. */
interface AttentionNotifier {
    /** Shows a system notification. */
    fun show(title: String, body: String)

    /** Shows [count] on the app's icon, or nothing when it is zero. */
    fun badge(count: Int)

}
