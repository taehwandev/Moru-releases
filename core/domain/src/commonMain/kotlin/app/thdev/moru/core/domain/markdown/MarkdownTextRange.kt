package app.thdev.moru.core.domain.markdown

/** Directional UTF16 source positions; adapters clamp incoming platform selections. */
data class MarkdownTextRange(val start: Int,val end: Int=start) {
    val min: Int get()=minOf(start,end)
    val max: Int get()=maxOf(start,end)
    val collapsed: Boolean get()=start==end
    companion object { val Zero=MarkdownTextRange(0) }
}
