package nl.rvantwisk.gatas.lib.extensions

import kotlin.test.Test
import kotlin.test.assertContentEquals

class CobsMessagesTest {

    @Test
    fun gdl90RoundTripPreservesRawPayload() {
        val payload = byteArrayOf(
            0x7E.toByte(),
            0x10.toByte(),
            0x00.toByte(),
            0x20.toByte(),
            0x00.toByte(),
        )

        val cobs = serializeGDL90V1(payload)
        val decoded = deserializeGDL90V1(cobs)

        assertContentEquals(payload, decoded)
    }
}
