package app.thdev.moru.feature.sessions.results

/**
 * What the agent's report says its checks proved: tests or a build that [passed] or not, with how many tests when it
 * says so. Read from the report's own words, so it is the agent's claim, shown as such, not Moru's verification.
 */
internal data class ResultCheck(val passed: Boolean, val count: Int?)

/** The check outcome [text] reports, or null when it names none. A failure anywhere outweighs a pass elsewhere. */
internal fun resultCheck(text: String): ResultCheck? {
    val lower = text.lowercase()
    FailureCount.findAll(lower).mapNotNull { it.groupValues.drop(1).firstOrNull(String::isNotEmpty)?.toCountOrNull() }
        .firstOrNull { it > 0 }?.let { return ResultCheck(passed = false, count = it) }
    if (FailedWords.containsMatchIn(lower)) return ResultCheck(passed = false, count = null)
    PassCount.findAll(lower).mapNotNull { it.groupValues.drop(1).firstOrNull(String::isNotEmpty)?.toCountOrNull() }
        .firstOrNull()?.let { return ResultCheck(passed = true, count = it) }
    if (PassedWords.containsMatchIn(lower)) return ResultCheck(passed = true, count = null)
    return null
}

private fun String.toCountOrNull(): Int? = replace(",", "").toIntOrNull()

private val FailureCount = Regex("""(\d[\d,]*)\s*(?:tests?\s+)?(?:failures?|failed|failing)\b|(\d[\d,]*)\s*개\s*실패|실패\s*(\d[\d,]*)""")
private val FailedWords = Regex("""build failed|tests? (?:are |is )?failing|tests? failed|테스트[^\n.]{0,12}실패|빌드[^\n.]{0,8}실패""")
private val PassCount = Regex("""(\d[\d,]*)\s*(?:tests?|checks?)\s*(?:all\s+)?(?:passed|pass(?:ing)?)\b|테스트\s*(\d[\d,]*)\s*개?[^\n.]{0,6}통과|(\d[\d,]*)\s*개(?:의)?\s*테스트[^\n.]{0,6}통과""")
private val PassedWords = Regex("""build successful|all tests pass|tests? (?:all )?pass(?:ed|ing)?\b|테스트[^\n.]{0,12}통과|빌드[^\n.]{0,8}(?:성공|통과)""")
