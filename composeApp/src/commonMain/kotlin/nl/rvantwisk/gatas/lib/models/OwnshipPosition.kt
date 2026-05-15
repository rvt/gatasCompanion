package nl.rvantwisk.gatas.lib.models

import kotlinx.serialization.Serializable

@Serializable
data class OwnshipPosition (
    val epoch : Int,
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val addressType: AddressType,
    val category: AircraftCategory,// wg type of aircraft
    val ellipsoidHeight: Int,      // In meters above the ellipse
    val track: Double,             // Degree
    val turnRate: Double,          // Degree/Sec
    val groundSpeed: Double,       // In meters per second
    val verticalRate: Double,      // In meters per second
)
