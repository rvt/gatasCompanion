package nl.rvantwisk.gatas.lib.models

enum class WifiMode(val value: UByte) {
    NC(0u),
    AP(1u),
    CLIENT(2u);

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromUByte(value: UByte): WifiMode? = map[value]
    }
}
