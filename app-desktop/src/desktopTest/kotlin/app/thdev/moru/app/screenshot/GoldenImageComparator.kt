package app.thdev.moru.app.screenshot

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.fail

/**
 * Compares a rendered image with a checked-in golden PNG.
 *
 * Goldens are only trusted on the machine class that recorded them (font
 * rendering differs by OS). Record or refresh with `MORU_UPDATE_GOLDEN=true`;
 * a missing golden otherwise fails instead of silently passing.
 */
class GoldenImageComparator(
    private val goldenDir: File = File("src/desktopTest/resources/golden"),
    private val actualDir: File = File("build/screenshot-diff"),
    private val updateGoldens: Boolean = System.getenv(UPDATE_ENV) == "true",
    private val maxChannelDelta: Int = 2,
    private val maxDiffRatio: Double = 0.0005,
) {
    fun assertMatches(name: String, actual: BufferedImage) {
        val golden = File(goldenDir, "$name.png")
        if (updateGoldens) {
            goldenDir.mkdirs()
            ImageIO.write(actual, "png", golden)
            return
        }
        if (!golden.exists()) {
            fail("Missing golden '$name'. Record it with $UPDATE_ENV=true, then review the PNG.")
        }
        val expected = ImageIO.read(golden)
        if (expected.width != actual.width || expected.height != actual.height) {
            writeActual(name, actual)
            fail(
                "Golden '$name' size ${expected.width}x${expected.height} " +
                    "but rendered ${actual.width}x${actual.height}. See ${actualFile(name)}",
            )
        }
        val ratio = differingPixelRatio(expected, actual)
        if (ratio > maxDiffRatio) {
            writeActual(name, actual)
            fail("Golden '$name' differs in ${"%.4f".format(ratio)} of pixels. See ${actualFile(name)}")
        }
        println("Golden '$name' matched (updateGoldens=$updateGoldens, diffRatio=${"%.6f".format(ratio)}, maxDiffRatio=$maxDiffRatio)")
    }

    private fun differingPixelRatio(expected: BufferedImage, actual: BufferedImage): Double {
        var differing = 0
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                if (channelsDiffer(expected.getRGB(x, y), actual.getRGB(x, y))) differing++
            }
        }
        return differing.toDouble() / (expected.width * expected.height)
    }

    private fun channelsDiffer(expectedArgb: Int, actualArgb: Int): Boolean {
        for (shift in CHANNEL_SHIFTS) {
            val delta = abs(((expectedArgb shr shift) and 0xFF) - ((actualArgb shr shift) and 0xFF))
            if (delta > maxChannelDelta) return true
        }
        return false
    }

    private fun writeActual(name: String, actual: BufferedImage) {
        actualDir.mkdirs()
        ImageIO.write(actual, "png", actualFile(name))
    }

    private fun actualFile(name: String) = File(actualDir, "$name.actual.png")

    private companion object {
        const val UPDATE_ENV = "MORU_UPDATE_GOLDEN"
        val CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)
    }
}
