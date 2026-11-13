package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ListeningPortsParserTest {
    @Test
    fun readsEachProcessWithItsLocalhostPorts() {
        val output = """
            p662
            f11
            n*:57485
            f12
            n*:57485
            p741
            f9
            n127.0.0.1:3000
            f10
            n[::1]:5173
            p9001
            n[::]:8080
        """.trimIndent()

        assertEquals(
            mapOf(662L to setOf(57485), 741L to setOf(3000, 5173), 9001L to setOf(8080)),
            parseListeningPorts(output),
        )
    }

    @Test
    fun aServerBoundToOneOutsideAddressCannotBeOpenedAsLocalhostAndIsLeftOut() {
        assertEquals(emptyMap(), parseListeningPorts("p1\nn192.168.1.5:3000\nn10.0.0.2:80\n"))
    }

    @Test
    fun anAgentCliListeningForItselfIsNotAServerToPreview() {
        val output = "p48291\nccodex\nn127.0.0.1:61301\np90741\ncagy-real\nn127.0.0.1:54656\np55827\ncnode\nn127.0.0.1:5173\n"

        assertEquals(mapOf(55827L to setOf(5173)), parseListeningPorts(output))
    }

    @Test
    fun garbageAndEmptyOutputGiveNothing() {
        assertEquals(emptyMap(), parseListeningPorts(""))
        assertEquals(emptyMap(), parseListeningPorts("n*:3000\n"))
        assertEquals(emptyMap(), parseListeningPorts("p12\nn*:notaport\nnno-colon\n"))
    }
}
