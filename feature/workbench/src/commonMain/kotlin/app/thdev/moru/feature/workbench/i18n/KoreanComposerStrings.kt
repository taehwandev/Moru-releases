package app.thdev.moru.feature.workbench.i18n

internal object KoreanComposerStrings : ComposerStrings {
    override fun labSession(agent: String) = "$agent 실험실"
    override val labBadge = "실험"
    override val headingTitle = "무엇으로 시작할까요?"
    override val headingBody = "시작할 대상을 고르세요. 전용 터미널이 바로 열리고, 요청은 거기서 입력하면 됩니다."
    override val preparingTask = "작업을 준비하는 중…"
    override val cancel = "취소"
}
