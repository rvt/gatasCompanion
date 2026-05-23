package nl.rvt.gatas.companion.services

import nl.rvantwisk.gatas.lib.models.OwnshipAircraftConfiguration

data class BridgeStatus(
    val running: Boolean = false,
    val connecting: Boolean = false,
    val bleConnected: Boolean = false,
    val udpHealthy: Boolean = false,
    val framesReceived: Long = 0,
    val framesRelayed: Long = 0,
    val bytesReceived: Long = 0,
    val bytesRelayed: Long = 0,
    val activeStream: String? = null,
    val availableStreams: String? = null,
    val serverActivityTick: Long = 0,
    val gatasActivityTick: Long = 0,
    val udpNmeaPackets: Long = 0,
    val udpCobsPackets: Long = 0,
    val udpNmeaActivityTick: Long = 0,
    val udpCobsActivityTick: Long = 0,
    val bleNmeaPackets: Long = 0,
    val bleCobsPackets: Long = 0,
    val bleNmeaActivityTick: Long = 0,
    val bleCobsActivityTick: Long = 0,
    val gdl90BridgeEnabled: Boolean = false,
    val gdl90FramesBridged: Long = 0,
    val gdl90BytesBridged: Long = 0,
    val gdl90ActivityTick: Long = 0,
    val ownshipConfiguration: OwnshipAircraftConfiguration? = null,
    val aircraftChangeTargetIcaoAddress: Long? = null,
    val lastEvent: String = "Idle",
    val lastError: String? = null,
) {
    val bridgeHealthy: Boolean
        get() = running && bleConnected && udpHealthy && lastError == null

    val udpPacketCount: Long
        get() = udpNmeaPackets + udpCobsPackets

    val blePacketCount: Long
        get() = bleNmeaPackets + bleCobsPackets
}
