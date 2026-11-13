package app.thdev.moru.core.ui.i18n

internal object KoreanCoreUiStrings : CoreUiStrings {
    override val binaryFile = "바이너리 파일이라 변경 내용을 표시하지 않습니다."
    override val renamedOnly = "내용은 그대로이고 이름만 바뀌었습니다."
    override val noLineChanges = "표시할 줄 변경이 없습니다. 권한 같은 속성만 바뀌었을 수 있습니다."
    override fun filesSummary(files: Int, additions: Int, deletions: Int, truncated: Boolean): String {
        val base = "파일 ${files}개 · +$additions −$deletions"
        return if (truncated) "$base · 변경이 커서 일부만 표시" else base
    }

    override val agoJustNow = "방금"
    override fun agoMinutes(count: Long) = "${count}분 전"
    override fun agoHours(count: Long) = "${count}시간 전"
    override fun agoDays(count: Long) = "${count}일 전"
    override fun agoWeeks(count: Long) = "${count}주 전"
    override fun agoMonths(count: Long) = "${count}개월 전"
    override fun agoYears(count: Long) = "${count}년 전"
}
