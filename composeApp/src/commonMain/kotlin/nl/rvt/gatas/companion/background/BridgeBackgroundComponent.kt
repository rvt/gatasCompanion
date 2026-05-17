package nl.rvt.gatas.companion.background

import kotlinx.coroutines.flow.StateFlow
import nl.rvt.gatas.companion.GaTasDevice
import nl.rvt.gatas.companion.services.BridgeStatus

expect object BridgeBackgroundComponent {
    val status: StateFlow<BridgeStatus>

    fun start(device: GaTasDevice)
    fun stop()
}
