package app.thdev.moru.feature.git.i18n

internal object KoreanHistoryStrings : HistoryStrings {
    override val title = "히스토리"
    override val refresh = "새로고침"
    override val loading = "히스토리를 불러오는 중…"
    override val noCommits = "아직 커밋이 없습니다."
    override fun commitCount(count: Int, truncated: Boolean): String {
        val base = "커밋 ${count}개"
        return if (truncated) "$base · 최근 이력만 표시" else base
    }
    override val diffNotConnected = "변경 내용 보기가 연결되지 않았습니다."
    override fun workingChanges(count: Int) = if (count == 0) "작업 폴더 · 변경 없음" else "작업 폴더 · 변경된 파일 ${count}개"
    override val workingFolder = "작업 폴더"
    override val columnGraph = "그래프"
    override val columnDescription = "설명"
    override val columnAuthor = "작성자"
    override val columnDate = "날짜"
    override val columnCommit = "커밋"

    override val diffLoading = "변경 내용을 불러오는 중…"
    override val noFileChanges = "이 커밋에는 파일 변경이 없습니다."
    override fun authorLine(name: String, email: String, isMerge: Boolean, parentCount: Int): String =
        "$name <$email> · ${if (isMerge) "병합 커밋" else "부모 ${parentCount}개"}"

    override val changesTitle = "변경 사항"
    override val changesSubtitle = "아직 커밋하지 않은 변경입니다. 에이전트가 작업하면 자동으로 갱신됩니다."
    override val changesLoading = "변경 사항을 불러오는 중…"
    override val noChanges = "커밋하지 않은 변경이 없습니다."

    override val resizeGitDetail = "Git 상세 패널 크기 조절"
    override val resetPaneSize = "패널 크기 초기화"
}
