package nl.rvantwisk.gatas.lib.models

import kotlinx.serialization.Serializable

@Serializable
data class OwnshipAircraftConfiguration(
    val gatasId: UInt,
    val options: UInt,
    val icaoAddress: Long,
    val newIcaoAddress: Long?,
    val icaoAddressList: List<Long>,
    val gatasIp: UInt,
    val pinCode: Int,
    val version: Int,
)



