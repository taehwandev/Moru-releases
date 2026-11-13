package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.feature.settings.onboarding.OnboardingStep

internal object KoreanOnboardingStrings : OnboardingStrings {
    override fun stepTitle(step: OnboardingStep) = when (step) {
        OnboardingStep.Intro -> "소개"
        OnboardingStep.Theme -> "테마"
        OnboardingStep.Done -> "완료"
    }
    override val back = "이전"
    override val next = "다음"
    override val skipAll = "설정 건너뛰기"
    override val start = "시작하기"
    override val finish = "Moru 시작하기"

    override val introTitle = "Moru에 오신 걸 환영해요"
    override val introBody = listOf(
        "코딩 에이전트가 작업마다 실제 터미널에서 일하는 곳이에요.",
        "결정이 필요하면 호박색으로 알려 주고 기다려요.",
        "먼저 몇 가지만 골라 주세요. 모두 나중에 바꿀 수 있어요.",
    )

    override val themeTitle = "테마를 골라 주세요"
    override val themeBody = "다른 테마는 설정 › 화면에 있어요."
    override val appleStyleTitle = "Apple 스타일"
    override val appleStyleBody = "Liquid Glass 표면과 둥근 컨트롤."
    override val doneTitle = "준비가 끝났어요"
    override val doneBody = "프로젝트 폴더를 열어 시작하세요. 고른 것은 설정에서 바꿀 수 있고, 이 안내도 설정에서 다시 볼 수 있어요."
    override val openProject = "프로젝트 열기…"
}
