package app.thdev.moru.feature.sessions.results

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResultCheckTest {
    @Test
    fun theReportsOwnWordsSayWhatItsChecksProved() {
        assertEquals(ResultCheck(true, 1523), resultCheck("1523 tests passed, 0 failures, 0 errors"))
        assertEquals(ResultCheck(true, 14), resultCheck("미리보기 링크를 바꿨고 테스트 14개가 통과했습니다."))
        assertEquals(ResultCheck(true, null), resultCheck("BUILD SUCCESSFUL in 41s"))
        assertEquals(ResultCheck(false, 2), resultCheck("12 tests passed, 2 failed"))
        assertEquals(ResultCheck(false, null), resultCheck("빌드가 실패했습니다. 로그를 확인해 주세요."))
        assertNull(resultCheck("사이드바 문구를 정리했습니다."))
    }
}
