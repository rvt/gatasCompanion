package nl.rvt.gatas.companion.services

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
    val lastEvent: String = "Idle",
    val lastError: String? = null,
) {
    val bridgeHealthy: Boolean
        get() = running && bleConnected && udpHealthy && lastError == null
}
