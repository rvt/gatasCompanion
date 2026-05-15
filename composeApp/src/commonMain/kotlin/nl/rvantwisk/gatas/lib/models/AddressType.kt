package nl.rvantwisk.gatas.lib.models

enum class AddressType(val value: UByte) {
    RANDOM(0u),   // Random ID, stealth or manually configured
    ICAO(1u),     // Official ICAO 24-bit aircraft address
    FLARM(2u),    // FLARM-assigned ID
    OGN(3u),      // OGN-assigned ID
    FANET(4u),    // FANET via ADS-L
    ADSL(5u),
    RESERVED(6u),
    UNKNOWN(7u);

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromUByte(value: UByte): AddressType = map[value] ?: UNKNOWN
    }
}
