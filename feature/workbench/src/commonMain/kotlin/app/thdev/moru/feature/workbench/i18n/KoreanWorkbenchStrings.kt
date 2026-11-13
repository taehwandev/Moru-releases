package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.feature.workbench.SidebarView
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * 60L
private const val WEEK_MINUTES = 7 * 24 * 60
private const val DAY_MINUTES = 24 * 60

internal object KoreanWorkbenchStrings : WorkbenchStrings {
    override val workspaceTitle = "작업 공간"
    override val openProjectButton = "프로젝트 열기…"
    override val reorderSidebarButton = "프로젝트 및 작업 순서 편집"
    override val finishReorderSidebarButton = "순서 편집 완료"
    override fun sidebarViewName(view: SidebarView) = when (view) {
        SidebarView.Projects -> "프로젝트"
        SidebarView.Board -> "현황"
        SidebarView.Results -> "모루"
    }
    override fun sidebarViewSwitcher(current: String) = "사이드바 보기: $current"
    override fun closedProjects(count: Int) = "닫은 프로젝트 · $count"
    override fun reopenProject(name: String) = "$name 다시 열기"
    override fun forgetClosedProject(name: String) = "$name 목록에서 삭제"
    override val forgetClosedWarning = "이 프로젝트와 작업을 목록에서 삭제할까요? 폴더와 파일은 그대로 남습니다."
    override val confirmForgetClosed = "목록에서 삭제"
    override val keepClosed = "유지"
    override val newTaskLabel = "새 작업"
    override fun newTaskIn(name: String) = "${name}에 새 작업"
    override val emptyTitle = "작업은 프로젝트에서 시작됩니다"
    override val emptyBody = "위의 버튼으로 폴더를 여세요. 그 폴더의 작업과 결과가 여기에 함께 모입니다."
    override val noTasksYet = "아직 작업이 없습니다. 선택한 프로젝트에서 시작해 보세요."

    override fun collapseProject(name: String) = "$name 접기"
    override fun expandProject(name: String) = "$name 펼치기"
    override fun closeProject(name: String) = "$name 닫기"
    override fun projectMenu(name: String) = "$name 프로젝트 메뉴"
    override fun taskMenu(title: String) = "$title 작업 메뉴"
    override val renameTask = "이름 바꾸기"
    override val changeProjectFolder = "폴더 위치 변경…"
    override val deleteTask = "삭제"
    override val openBeside = "옆에 열기"
    override val closeBeside = "옆에서 닫기"
    override val openInNewWindow = "새 창으로 열기"
    override val resizeBeside = "옆 작업 크기 조절"
    override val switchToTask = "이 작업으로 전환"
    override val deleteTaskWarning = "이 작업을 삭제할까요? 터미널이 멈추고 목록에서 사라집니다. 파일은 그대로 남습니다."
    override val confirmDeleteTask = "작업 삭제"
    override val keepTask = "유지"
    override fun renameProject(name: String) = "$name 이름 바꾸기"
    override val saveProjectName = "이름 저장"
    override val cancelRename = "이름 바꾸기 취소"
    override val resumeLabel = "직전 세션 이어서 실행"
    override fun agentExited(agent: String) = "$agent 이(가) 종료되었습니다."
    override val resumeHere = "여기서 이어서 실행"
    override val dismiss = "닫기"
    override fun agentUpdated(agent: String) = "$agent 이(가) 업데이트되었습니다. 새 버전을 쓰려면 다시 시작하세요."
    override val restartAgent = "다시 시작"
    override fun codexThemeChanged(title: String, sessionId: String) = "Codex 테마 변경됨 · $title ($sessionId)"
    override fun codexThemeRestartDetails(knownConversation: Boolean) =
        if (knownConversation) "테마를 적용하고 같은 터미널에서 대화를 이어가려면 다시 시작하세요. 전송하지 않은 입력은 사라질 수 있습니다."
        else "테마를 적용하려면 다시 시작하세요. 이 터미널에서 Codex 대화 선택기가 열립니다. 전송하지 않은 입력은 사라질 수 있습니다."
    override val themeRestartBusy = "Codex가 작업을 마치거나 입력 대기를 끝낼 때까지 기다려 주세요."
    override val themeRestartInProgress = "Codex 다시 시작하는 중…"
    override val themeRestartFailed = "Codex를 안전하게 다시 시작하지 못했습니다. 세션이 대기 상태일 때 다시 시도하세요."
    override val restartForTheme = "테마 적용을 위해 다시 시작"
    override fun resumeSessionTitle(agent: String) = "$agent · 이어서"
    override fun closeStopsRunning(count: Int) = "에이전트 ${count}개가 아직 실행 중입니다. 닫으면 멈춥니다."
    override val closeAndStop = "멈추고 닫기"
    override val keepOpen = "계속 열어 두기"
    override val closeTerminalPrompt = "안에서 실행 중인 작업이 멈춥니다."
    override fun pressAgainToClose(shortcut: String) = "$shortcut 한 번 더 누르면 닫기"
    override fun closeTerminalTitle(title: String?) = if (title.isNullOrBlank()) "터미널을 닫을까요?" else "$title 터미널을 닫을까요?"
    override fun reopenAfterClose(shortcut: String) = "$shortcut 다시 열기"
    override fun projectDescription(name: String) = "프로젝트 $name"
    override fun reorderProject(name: String) = "$name 프로젝트 순서 변경. 드래그하거나 Alt+위 또는 Alt+아래 방향키를 사용하세요."
    override fun reorderTask(title: String) = "$title 작업 순서 변경. 드래그하거나 Alt+위 또는 Alt+아래 방향키를 사용하세요."
    override val moveSidebarItemUp = "위로 이동"
    override val moveSidebarItemDown = "아래로 이동"
    override val folderUnavailable = "폴더를 사용할 수 없음"
    override val noFolderLinked = "연결된 폴더 없음"
    override fun tasksNeedYou(count: Int, project: String) = "$project 에서 작업 ${count}개가 확인 필요"
    override fun taskDescription(title: String) = "작업 $title"
    override fun taskSessions(total: Int, working: Int, needsYou: Int) = buildList {
        add("세션 ${total}개")
        if (working > 0) add("활성 $working")
        if (needsYou > 0) add("확인 필요 $needsYou")
    }.joinToString(" · ")
    override val terminalLabel = "터미널"
    override fun sessionRunning(count: Int) = "진행 중 $count"
    override fun sessionNeedsAttention(count: Int) = "확인 필요 $count"
    override fun sessionResponsesReady(count: Int) = "응답 완료 $count"
    override fun finished(count: Int?) = if (count == null) "완료" else "완료 $count"
    override fun sessionAwaitingReply(count: Int) = "답변 대기 $count"
    override fun awaitingReply(count: Int?) = if (count == null) "답변 대기" else "답변 대기 $count"
    override fun sessionWaiting(count: Int) = "대기 중 $count"
    override fun sessionEnded(count: Int) = "종료 $count"

    override fun statusLabel(state: TaskDisplayState) = when (state) {
        TaskDisplayState.Understanding -> "요청 파악 중"
        TaskDisplayState.Preparing -> "준비 중"
        TaskDisplayState.Working -> "작업 중"
        TaskDisplayState.Checking -> "검증 중"
        TaskDisplayState.NeedsInput -> "입력 필요"
        TaskDisplayState.NeedsApproval -> "승인 필요"
        TaskDisplayState.Done -> "완료"
        TaskDisplayState.Waiting -> "대기 중"
        TaskDisplayState.Stopped -> "중단됨"
        TaskDisplayState.Failed -> "실패"
        TaskDisplayState.Unknown -> "상태 확인 불가"
    }

    override fun sectionLabel(group: TaskGroup) = when (group) {
        TaskGroup.NeedsAttention -> "내 차례"
        TaskGroup.Running -> "진행 중"
        TaskGroup.Review -> "중단됨"
        TaskGroup.Done -> "끝난 작업"
    }

    override fun waiting(count: Int) = "대기 $count"
    override fun working(count: Int) = "작업 중 $count"
    override val noActiveTasks = "진행 중인 작업 없음"
}
