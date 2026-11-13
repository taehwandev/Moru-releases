package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
internal object KoreanCodeStrings : CodeStrings {
    override val explorerTitle = "탐색기"
    override val newNote = "새 노트"
    override val closeNewNote = "새 노트 닫기"
    override val quickOpenPlaceholder = "빠른 열기"
    override val noMatches = "일치하는 항목이 없습니다"
    override val newNotePlaceholder = "새 노트 이름"
    override val create = "만들기"
    override val collapseAll = "모든 폴더 접기"
    override val expandAll = "모든 폴더 펼치기"
    override val locateOpenFile = "열린 파일 위치로 이동"
    override val explorerOptions = "탐색기 옵션"
    override val compactRows = "촘촘하게 보기"
    override val followOpenFile = "열린 파일 항상 표시"
    override val sortByName = "이름순 정렬"
    override val sortByType = "종류순 정렬"
    override val foldersFirst = "폴더 먼저"
    override val showIgnored = "무시된 파일 보기"
    override val mergeSingleFolders = "단일 폴더 경로 합치기"

    override val rename = "이름 변경"
    override val confirmDelete = "삭제 확인"
    override val cancel = "취소"
    override val delete = "삭제"
    override val close = "x"
    override val newPathPlaceholder = "새 경로/이름"
    override val save = "저장"

    override val edit = "편집"
    override val read = "읽기"
    override val source = "소스"
    override val htmlPreview = "미리보기"
    override val htmlRefresh = "새로고침"
    override val htmlStarting = "HTML 미리보기를 여는 중…"
    override val htmlChromeMissing = "HTML 미리보기를 사용하려면 Chrome을 설치하세요. 소스는 계속 보고 편집할 수 있습니다."
    override val htmlFailed = "HTML 미리보기를 열지 못했습니다. 새로고침하거나 소스로 전환하세요."
    override val unknownSize = "크기 알 수 없음"
    override fun savedBytes(count: Long) = "${count}바이트를 저장했습니다"
    override val selectFilePrompt = "왼쪽에서 파일을 고르면 여기에 코드가 열립니다."
    override fun previewKind(status: WorkbenchFilePreviewStatus) = when (status) {
        WorkbenchFilePreviewStatus.TEXT -> "텍스트"
        WorkbenchFilePreviewStatus.BINARY -> "바이너리"
        WorkbenchFilePreviewStatus.OVERSIZED -> "큰 파일"
        WorkbenchFilePreviewStatus.MISSING -> "없음"
        WorkbenchFilePreviewStatus.UNAVAILABLE -> "열 수 없음"
    }
    override fun previewMessage(status: WorkbenchFilePreviewStatus) = when (status) {
        WorkbenchFilePreviewStatus.TEXT -> "미리 볼 내용이 없습니다."
        WorkbenchFilePreviewStatus.BINARY -> "바이너리 파일은 화면에 표시하지 않습니다."
        WorkbenchFilePreviewStatus.OVERSIZED -> "큰 파일은 정보만 보여 줍니다."
        WorkbenchFilePreviewStatus.MISSING -> "파일이 더 이상 없습니다."
        WorkbenchFilePreviewStatus.UNAVAILABLE -> "이 파일은 미리 볼 수 없습니다."
    }

    override val gitTitle = "GIT"
    override val refresh = "새로고침"
    override fun changedCount(count: Int) = "${count}개 변경"
    override val clean = "깨끗함"
    override fun gitSummary(staged: Int, unstaged: Int, untracked: Int) =
        "스테이징 $staged / 미스테이징 $unstaged / 추적 안 함 $untracked"

}
