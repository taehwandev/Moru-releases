package app.thdev.moru.feature.sessions.i18n

import app.thdev.moru.core.model.session.SessionActivity

internal object KoreanSessionsStrings : SessionsStrings {
    override val title = "세션"
    override fun summary(total: Int, working: Int, needsCheck: Int) =
        "세션 ${total}개 · 작업 중 $working · 확인 필요 $needsCheck"
    override val empty = "실행 중인 세션이 없습니다."
    override val unattachedGroup = "작업에 속하지 않은 세션"
    override val scopeAll = "모든 프로젝트"
    override val scopeThisProject = "이 프로젝트"
    override fun exitCode(code: Int) = "종료 코드 $code"
    override val runningProgram = "실행 중"
    override fun openPreview(address: String) = "$address 미리보기"
    override val terminal = "터미널"
    override val webPreview = "웹 미리보기"
    override fun activityLabel(activity: SessionActivity) = when (activity) {
        SessionActivity.Starting -> "시작 중"
        SessionActivity.Working -> "작업 중"
        SessionActivity.Asking -> "답변 필요"
        SessionActivity.Quiet -> "응답 완료"
        SessionActivity.Waiting -> "대기 중"
        SessionActivity.Idle -> "셸 대기"
        SessionActivity.Exited -> "종료됨"
        SessionActivity.Failed -> "실패"
    }
    override val permissionRequestLabel = "승인 필요"
    override fun activityHint(activity: SessionActivity): String? = when (activity) {
        SessionActivity.Asking -> "답을 기다리고 있습니다"
        SessionActivity.Quiet -> "에이전트가 출력을 멈췄습니다. 이번 응답을 마쳤을 수 있지만 작업 목표 달성 여부는 알 수 없습니다"
        SessionActivity.Waiting -> "에이전트가 다음 지시를 기다립니다"
        SessionActivity.Idle -> "셸이 프롬프트에서 대기 중이며 실행 중인 것이 없습니다"
        SessionActivity.Exited -> "정상 종료했지만 목표 달성 여부는 알 수 없습니다"
        SessionActivity.Starting, SessionActivity.Working, SessionActivity.Failed -> null
    }
    override fun rowDescription(agent: String, title: String, activity: String) = "$agent, $title, $activity"
    override val boardNeedsAttention = "확인 필요"
    override val boardAwaitingReply = "답변 대기"
    override val awaitingReplyLabel = "답변 대기"
    override val boardRunning = "진행 중"
    override val boardCompleted = "응답 완료"
    override val boardReady = "대기 중"
    override val boardNothingNeedsAttention = "확인할 항목이 없습니다."
    override val boardNothingRunning = "돌아가는 작업이 없습니다."
    override val boardNothingCompleted = "완료된 응답이 없습니다."
    override val boardNothingReady = "대기 중인 세션이 없습니다."
    override val resultsAsking = "답이 필요해요"
    override val resultsDone = "완료된 결과"
    override fun resultsSeen(count: Int) = "확인한 결과 · $count"
    override val resultsEmpty = "아직 끝난 작업이 없습니다. 에이전트가 일을 마치면 결과가 여기에 모입니다."
    override val resultsYou = "나"
    override val resultsTitle = "모루"
    override val resultsStatusDone = "완료"
    override val resultsStatusAsking = "답을 기다려요"
    override fun resultsSummary(asking: Int, done: Int) = listOfNotNull(
        "답 필요 $asking".takeIf { asking > 0 }, "완료 $done".takeIf { done > 0 },
    ).joinToString(" · ")
    override val resultsReplyPlaceholder = "답을 입력하고 Enter"
    override val resultsContinuePlaceholder = "이어서 할 일을 입력하고 Enter"
    override val resultsSend = "보내기"
    override val resultsSendFailed = "보내지 못했습니다. 세션을 열어 확인해 주세요."
    override val resultsOpen = "열기"
    override val resultsContinue = "이어서"
    override val resultsMarkSeen = "확인"
    override val resultsYes = "예"
    override val resultsNo = "아니오"
    override val resultsMore = "더 보기"
    override val resultsLess = "접기"
    override val resultsFailed = "실패했습니다"
    override fun resultsUncommitted(count: Int) = "커밋 전 변경 ${count}개"
    override fun resultsChecksPassed(count: Int?) = if (count == null) "검사 통과" else "테스트 ${count}개 통과"
    override fun resultsChecksFailed(count: Int?) = if (count == null) "검사 실패" else "${count}개 실패"
}
