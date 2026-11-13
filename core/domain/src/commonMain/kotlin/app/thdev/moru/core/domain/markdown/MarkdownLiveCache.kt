package app.thdev.moru.core.domain.markdown

/** Per-editor bounded semantic cache; native/Compose colors are applied by their adapters. */
class MarkdownLiveCache {
    private var lastSource: String?=null
    private var lastReveal: IntRange?=null
    private var last: MarkdownLiveResult?=null
    private val lines=HashMap<Pair<String,Boolean>,LineDecoration>()
    internal fun whole(source: String,reveal: IntRange): MarkdownLiveResult? =
        last.takeIf { source==lastSource && reveal==lastReveal }
    internal fun keep(source: String,reveal: IntRange,result: MarkdownLiveResult) {
        lastSource=source;lastReveal=reveal;last=result
    }
    internal fun line(line: String,active: Boolean,decorate: ()->LineDecoration): LineDecoration {
        if (lines.size>20_000) lines.clear()
        return lines.getOrPut(line to active,decorate)
    }
    internal class LineDecoration(private val hidden: BooleanArray,private val styles: List<MarkdownLiveResult.Span>) {
        fun applyAt(offset: Int,into: BooleanArray,intoStyles: MutableList<MarkdownLiveResult.Span>) {
            for (i in hidden.indices) if (hidden[i]) into[offset+i]=true
            styles.forEach { intoStyles+=MarkdownLiveResult.Span(it.start+offset,it.end+offset,it.style) }
        }
    }
}
