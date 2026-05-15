package nl.rvantwisk.gatas.lib.models

enum class AircraftCategory(val value: UByte) {
    UNKNOWN(0u),
    LIGHT(1u),
    SMALL(2u),
    LARGE(3u),
    HIGH_VORTEX(4u),
    HEAVY_ICAO(5u),
    AEROBATIC(6u),
    ROTORCRAFT(7u),
    GLIDER(9u),
    LIGHT_THAN_AIR(10u),
    SKY_DIVER(11u),
    ULTRA_LIGHT_FIXED_WING(12u),
    UN_MANNED(14u),
    SPACE_VEHICLE(15u),
    SURFACE_EMERGENCY_VEHICLE(17u),
    SURFACE_VEHICLE(18u),
    POINT_OBSTACLE(19u),
    CLUSTER_OBSTACLE(20u),
    LINE_OBSTACLE(21u),
    GYROCOPTER(40u),
    HANG_GLIDER(41u),
    PARA_GLIDER(42u),
    DROP_PLANE(43u),
    MILITARY(44u);

    companion object {
        private val map = AircraftCategory.entries.associateBy { it.value }
        fun fromUByte(value: UByte): AircraftCategory = map[value] ?: UNKNOWN
    }
}
