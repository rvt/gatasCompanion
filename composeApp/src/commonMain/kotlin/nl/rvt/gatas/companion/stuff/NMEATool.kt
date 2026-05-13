package nl.rvt.gatas.companion.stuff

import co.touchlab.kermit.Logger
import kotlin.math.roundToInt

private val log = Logger.withTag("NMEATool")

fun calculateNmeaChecksum(sentence: String): String {
    val data = sentence.substring(1).takeWhile { it != '*' }
    val checksum = data.fold(0) { acc, char -> acc xor char.code }
    val hexChecksum = checksum.toString(16).uppercase().padStart(2, '0')
    return "$sentence*${hexChecksum}\r\n"
}

