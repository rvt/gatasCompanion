package nl.rvantwisk.gatas.lib.models

enum class DataSource(val value: UByte) {
    FLARM(0u),
    ADSLM(1u),
    ADSLO_HDR(2u),
    FANET(3u),
    OGN(4u),

    // Special marker for protocol grouping (same value as PAW)
    _TRANSPROTOCOLS(4u),
    ADSB(5u),
    MLAT(6u),

    // Number of actual usable items
    _ITEMS(7u),

    // Only for fallback parsing
    UNKNOWN(255u);

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromUByte(value: UByte): DataSource = map[value] ?: UNKNOWN
    }
}
