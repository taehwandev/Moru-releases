package app.thdev.moru.feature.sessions.results

/**
 * True when [question] can be answered with a plain yes or no ("Should I also update the rail?", "레일도 바꿀까요?"),
 * so the card can offer those two answers; a question asking which, what, or how is answered in words.
 */
internal fun isYesNoQuestion(question: String): Boolean {
    val text = question.trim().lowercase()
    if (OpenQuestion.containsMatchIn(text)) return false
    return EnglishYesNo.containsMatchIn(text) || KoreanYesNo.containsMatchIn(text)
}

private val OpenQuestion = Regex("""\b(which|what|how|where|when|who|why)\b|어떤|어느|무엇|뭐|뭘|어디|언제|누가|왜|어떻게|몇""")
private val EnglishYesNo = Regex("""^(should|shall|do|does|did|can|could|would|will|is|are|want|ok|okay)\b""")
private val KoreanYesNo = Regex("""(까요|할래요|될까요|맞나요|맞습니까|괜찮을까요|좋을까요|할까|ㄹ까요)[?？]$""")
