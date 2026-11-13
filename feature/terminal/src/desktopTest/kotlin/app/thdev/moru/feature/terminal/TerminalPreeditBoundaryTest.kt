package app.thdev.moru.feature.terminal

import java.awt.font.TextHitInfo
import java.io.File
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalPreeditBoundaryTest {
    @Test
    fun mixedAndConsecutiveSyllablesAdvanceBeforeThePtyEchoArrives() {
        PreeditTestFixture().use { fixture ->
            fixture.composition("하")
            val first = fixture.candidateLocation()
            fixture.composition("한ㄱ", committedCharacters = 1)
            assertEquals(first.x + 2 * fixture.cellWidth, fixture.candidateLocation().x)
            fixture.composition("글", committedCharacters = 1)
            fixture.composition("ㅇ")
            assertEquals(first.x + 4 * fixture.cellWidth, fixture.candidateLocation().x)
            fixture.composition("어", committedCharacters = 1)
            assertTrue(fixture.connector.awaitText("한글어"))
        }
    }

    @Test
    fun aNewInputLocationAfterCommitDoesNotReuseThePreviousPrompt() {
        PreeditTestFixture().use { fixture ->
            fixture.composition("한", committedCharacters = 1)
            fixture.output("\u001B[5;20H")
            val newPrompt = fixture.candidateLocation()
            fixture.composition("글")
            assertEquals(newPrompt, fixture.candidateLocation())
        }
    }

    @Test
    fun widePreeditWrapsWithoutClippingAndCandidateOffsetsUseCellWidths() {
        PreeditTestFixture().use { fixture ->
            fixture.output("\u001B[3;80H")
            fixture.composition("한a")
            val start = fixture.candidateLocation()
            assertEquals(4, start.x, "A two-cell glyph at the last column must move to the next row.")
            var afterHangul = start
            SwingUtilities.invokeAndWait {
                afterHangul = fixture.panel.inputMethodRequests.getTextLocation(TextHitInfo.leading(1))
            }
            assertEquals(start.x + fixture.cellWidth * 2, afterHangul.x)
            assertEquals(start.y, afterHangul.y)
        }
    }

    @Test
    fun nativePreeditRendersInBothThemesAndAtSmallAndLargeFontSizes() {
        val directory = File("build/reports/terminal-preedit").apply { mkdirs() }
        for (dark in listOf(false, true)) {
            for (size in listOf(10, 24)) {
                PreeditTestFixture(fontSize = size, dark = dark).use { fixture ->
                    val empty = fixture.paint()
                    fixture.composition("한글 abc e\u0301 😀")
                    val image = fixture.paint()
                    val changed = (0 until image.width).count { x ->
                        (0 until image.height).any { y -> image.getRGB(x, y) != empty.getRGB(x, y) }
                    }
                    assertTrue(changed >= fixture.cellWidth * 10, "The localized composition must be visible at font size $size.")
                    assertTrue(ImageIO.write(image, "png", File(directory, "${if (dark) "dark" else "light"}-$size.png")))
                }
            }
        }
    }
}
