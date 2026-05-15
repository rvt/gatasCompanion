package nl.rvantwisk.gatas.lib.models

enum class DataSource(val value: UByte) {
    FLARM(0u),
    ADSL(1u),
    FANET(2u),
    OGN1(3u),

    // Special marker for protocol grouping (same value as PAW)
    _TRANSPROTOCOLS(4u),

    PAW(4u),
    ADSB(5u),

    // Number of actual usable items
    _ITEMS(6u),

    // Only for fallback parsing
    UNKNOWN(255u);

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromUByte(value: UByte): DataSource = map[value] ?: UNKNOWN
    }
}
