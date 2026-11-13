package app.thdev.moru.feature.codepanel.i18n

internal object KoreanCodePanelStrings : CodePanelStrings {
    override val worktree = "워크트리"
    override fun mainWorktree(label: String) = "$label · 프로젝트 폴더"
    override val loading = "변경 사항을 읽는 중…"
    override val noChanges = "이 워크트리에는 아직 변경이 없습니다."
    override val notRepository = "이 폴더는 Git 저장소가 아닙니다."
    override val codeMode = "코드"
    override val diffMode = "Diff"
    override fun unchangedLines(count: Int) = "변경 없는 ${count}줄"
}
