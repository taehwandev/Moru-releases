package app.thdev.moru.app.i18n

import app.thdev.moru.app.ProjectTab
import app.thdev.moru.app.MoruDestination

internal object KoreanAppStrings : AppStrings {
    override fun destinationLabel(destination: MoruDestination) = when (destination) {
        MoruDestination.Workbench -> "워크벤치"
        MoruDestination.Knowledge -> "지식"
        MoruDestination.Notifications -> "알림"
        MoruDestination.Settings -> "설정"
    }

    override fun tabLabel(tab: ProjectTab) = when (tab) {
        ProjectTab.Work -> "작업"
        ProjectTab.Code -> "코드"
        ProjectTab.Git -> "Git"
    }

    override val selectProject = "프로젝트를 선택하세요"
    override val newTask = "새 작업"
    override val menuFile = "파일"
    override val menuView = "보기"
    override val menuProject = "프로젝트"
    override val menuOpenProject = "프로젝트 열기…"
    override val menuNewTask = "새 작업"
    override val menuCloseProject = "프로젝트 닫기"
    override val menuQuitEndingTerminals = "종료하고 터미널 모두 끄기"
    override val menuNewSession = "새 세션"
    override val menuSplitRight = "터미널 오른쪽으로 나누기"
    override val menuSplitDown = "터미널 아래로 나누기"
    override val menuCloseTerminal = "터미널 닫기"
    override val menuReopenClosedTerminal = "닫은 터미널 다시 열기"
    override val menuWork = "작업"
    override val menuCode = "코드"
    override val menuGit = "Git"
    override val menuSettings = "설정"
    override val menuNextTask = "다음 작업"
    override val menuPreviousTask = "이전 작업"
    override val menuNextSession = "다음 세션"
    override val menuPreviousSession = "이전 세션"
    override val menuNextProject = "다음 프로젝트"
    override val menuPreviousProject = "이전 프로젝트"
    override val menuKnowledge = "지식"
    override val menuCheckForUpdates = "업데이트 확인…"
    override val notificationTitle = "Moru"
    override fun attentionFailed(task: String) = "$task 작업이 실패했습니다."
    override fun attentionAsking(task: String, agent: String?, question: String) =
        (if (agent != null) "$task 작업의 $agent 가 확인을 요청합니다" else "$task 작업이 확인을 요청합니다") +
            (if (question.isNotEmpty()) ": $question" else ".")
    override fun attentionFinished(task: String, agent: String) = "$task 작업의 $agent 가 작업을 마쳤습니다. 결과를 확인해 주세요."
    override fun closeNeedsConfirm(count: Int) =
        "에이전트 ${count}개가 아직 실행 중입니다. 멈추고 닫으려면 사이드바의 닫기 표시를 누르세요."
    override val quitTitle = "Moru를 종료할까요?"
    override fun quitEndsTerminals(count: Int) =
        "터미널 ${count}개가 실행 중입니다. '마지막 상태 기억'이 꺼져 있어서 종료하면 모든 터미널과 그 안의 에이전트가 함께 종료됩니다."
    override val quitKeepTerminals = "이번에는 터미널 유지"
    override val quitCancel = "취소"
    override val quitConfirm = "종료"
    override val startupFailedTitle = "Moru를 시작할 수 없습니다"
    override val startupReportIssue = "문제 보고…"
    override val startupClose = "닫기"
    override val crashTitle = "Moru에 문제가 생겼습니다"
    override fun crashBody(reason: String, log: String) =
        "Moru가 처리하지 못한 오류가 났습니다: $reason\n\n계속 쓸 수 있지만 이상하게 동작하면 Moru를 다시 시작하세요. " +
            "보고해 주시면 고치는 데 도움이 됩니다. 자세한 내용은 $log 에 있습니다."
    override val crashLastTimeTitle = "Moru가 예기치 않게 종료되었습니다"
    override fun crashLastTime(reason: String, log: String) =
        "지난번에 Moru가 오류로 종료되었습니다: $reason\n\n보고해 주시면 고치는 데 도움이 됩니다. 자세한 내용은 $log 에 있습니다."
    override fun startupFailed(reason: String, log: String) =
        "Moru를 시작할 수 없습니다: $reason\n\n자세한 내용은 $log 에 있습니다."
    override fun startupNeedsNewerMoru(log: String) =
        "프로젝트와 작업이 더 새로운 버전의 Moru로 저장되어 있어 이 버전으로는 열 수 없습니다. " +
            "최신 Moru를 설치하면 그대로 이어서 쓸 수 있으며, 지워진 것은 없습니다.\n\n자세한 내용은 $log 에 있습니다."

    override val noProjectSelected = "선택된 프로젝트 없음"
    override fun readyForTask(project: String) = "$project › 작업 준비됨"
    override fun branchDetail(branch: String, place: String) = "브랜치 $branch · $place"
    override fun folderDetail(place: String) = "프로젝트 폴더 · $place"

    override val welcomeEmptyTitle = "다음 작업을 위한 공간"
    override val welcomeEmptyBody = "폴더를 열고 할 일을 적으면, 에이전트와 변경 사항이 한곳에 모입니다."
    override val welcomeProjectBody = "바꾸고 싶은 것을 설명하세요. 작업, 터미널, 변경 내용이 여기에 함께 유지됩니다."
    override val openProject = "프로젝트 열기"
    override val terminalHint = "터미널에서: Moru --project /path/to/project"
    override val unavailableTitle = "프로젝트 폴더를 사용할 수 없음"
    override fun unavailableBody(name: String) =
        "$name 폴더에 접근할 수 없습니다. 이름을 바꾸거나 옮겼다면 폴더를 찾아 연결하면 작업이 그대로 유지됩니다."
    override val findProjectFolder = "폴더 찾기"
    override val findProjectFolderTitle = "프로젝트 폴더 선택"
    override val projectFolderInUse = "그 폴더는 이미 다른 프로젝트입니다. 이 프로젝트를 옮기려면 그 프로젝트를 먼저 닫고 삭제하세요."
    override val projectFolderUnavailable = "그 폴더에 접근할 수 없습니다."
    override fun projectInOwnWindowTitle(name: String) = "$name 프로젝트는 자기 창에 열려 있어요"
    override val projectInOwnWindowBody = "터미널과 결과는 그 창에 있어요."
    override val bringWindowForward = "그 창 앞으로 가져오기"
    override val moveToMainWindow = "메인 창으로 옮기기"

    override val preparingTask = "작업을 준비하는 중…"
    override val taskCouldNotStart = "작업을 시작하지 못했습니다"
    override val noAgentLaunched = "에이전트가 실행되지 않았습니다. 준비를 다시 시도하거나 프로젝트 폴더를 직접 사용하세요."
    override val retry = "다시 시도"
    override val switchWhilePreparing = "작업 공간을 준비하는 동안 다른 작업으로 전환할 수 있습니다."
    override val taskBrief = "작업 설명"
    override val hideTaskBrief = "작업 설명 접기"

    override val dismiss = "닫기"
    override val loadingProjectFiles = "프로젝트 파일 불러오는 중…"

    override val updateChecking = "업데이트 확인 중…"
    override fun updateUpToDate(version: String) = "Moru $version 이 최신 버전입니다."
    override fun updateDownloading(version: String, percent: Int) = "Moru $version 다운로드 중… $percent%"
    override fun updateReady(version: String) = "Moru $version 업데이트가 준비되었습니다."
    override val updateRestart = "재시작하여 업데이트"
    override val updateLater = "나중에"
    override fun updateInstalling(version: String) = "Moru $version 설치 중…"
    override fun updateFailed(message: String) = "Moru를 업데이트하지 못했습니다: $message"
    override val unknownError = "알 수 없는 오류"
    override fun couldNotChooseFolder(message: String) = "폴더를 선택하지 못했습니다: $message"
    override fun couldNotOpenProject(message: String) = "프로젝트를 열지 못했습니다: $message"
    override val chooseWorkflowFolder = "워크플로우 폴더 선택"
    override fun couldNotStart(title: String, message: String) = "\"$title\" 작업을 시작하지 못했습니다: $message"
}
