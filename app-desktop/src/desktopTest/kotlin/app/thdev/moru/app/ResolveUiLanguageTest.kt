package app.thdev.moru.app

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

/** The language setting against the computer's language tag. */
class ResolveUiLanguageTest {
    @Test
    fun aKoreanComputerGetsKoreanWhenTheSettingFollowsTheSystem() {
        listOf("ko", "ko-KR", "ko_KR", "KO").forEach { tag ->
            assertEquals(UiLanguage.Korean, resolveUiLanguage(AppLanguage.System, tag), tag)
        }
    }

    @Test
    fun anyOtherComputerLanguageGetsEnglishBecauseThereIsNoTextForIt() {
        listOf("en", "en-US", "ja-JP", "de", "", "und", "zh-Hans-CN").forEach { tag ->
            assertEquals(UiLanguage.English, resolveUiLanguage(AppLanguage.System, tag), tag)
        }
    }

    @Test
    fun anExplicitChoiceWinsWhateverTheComputerSpeaks() {
        assertEquals(UiLanguage.English, resolveUiLanguage(AppLanguage.English, "ko-KR"))
        assertEquals(UiLanguage.Korean, resolveUiLanguage(AppLanguage.Korean, "en-US"))
    }

    @Test
    fun aLanguageThatOnlyStartsWithKoIsNotKorean() {
        assertEquals(UiLanguage.English, resolveUiLanguage(AppLanguage.System, "kok-IN"))
    }
}
