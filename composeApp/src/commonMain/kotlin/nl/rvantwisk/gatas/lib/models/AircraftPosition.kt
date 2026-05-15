package nl.rvantwisk.gatas.lib.models

import kotlinx.serialization.Serializable


@Serializable
data class AircraftPosition (
    val id: UInt,
    val dataSource: DataSource,
    val addressType: AddressType,
    val latitude: Double,
    val longitude: Double,
    val course: Double,             // Degree
    val hTurnRate: Double,          // Degree per second
    val groundSpeed: Double,        // In meters per second
    val verticalSpeed: Double,      // In meters per second
    val aircraftCategory: AircraftCategory,

    val callSign: String,
    val qnh: Double?,


    val nicBaro: Int,               // When 1 cross checked and correct
    var ellipsoidHeight: Int?,      // In meters if send from the webservice
    val baroAltitude: Int?,         // In meters if send from the webservice. null if isGround is true
    val isGround: Boolean,
)
