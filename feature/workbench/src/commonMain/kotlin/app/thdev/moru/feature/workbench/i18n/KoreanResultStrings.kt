package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.feature.workbench.result.ResultTab

internal object KoreanResultStrings : ResultStrings {
    override fun tab(tab: ResultTab) = when (tab) {
        ResultTab.Code -> "코드"
    }
    override val needsTask = "이 도구는 작업을 열거나 시작하면 쓸 수 있습니다."
    override val openResult = "결과 패널 열기"
    override val resizeResult = "결과 패널 크기 조절"
    override fun resultOf(taskTitle: String) = "$taskTitle 결과"
    override val detachResult = "결과를 별도 창으로 열기"
    override val dockResult = "결과를 메인 창으로 되돌리기"
    override fun closeTool(tool: ResultTab) = "${tab(tool)} 닫기"
    override val detachedResult = "결과가 별도 창에 열려 있습니다"
    override fun detachedTitle(task: String) = "$task · 결과"
    override val resetPaneSize = "패널 크기 초기화"
    override val noChanges = "아직 변경이 없습니다. 에이전트가 고친 내용이 여기에 나타납니다."
    override val codeNotRepository = "이 폴더는 Git 저장소가 아닙니다."
    override val reviseLabel = "다르게 하고 싶은가요?"
    override val revisePlaceholder = "무엇을 바꿀지 적어 주세요"
    override val reviseButton = "에이전트에게 요청"
    override val revised = "에이전트 터미널에 입력해 두었습니다. 거기서 Enter를 눌러 전송하세요."
    override val reviseNoAgent = "이 작업에 실행 중인 에이전트 터미널이 없어서 입력하지 못했습니다."
}
