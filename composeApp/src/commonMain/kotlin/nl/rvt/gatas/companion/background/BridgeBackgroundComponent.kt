package nl.rvt.gatas.companion.background

import kotlinx.coroutines.flow.StateFlow
import nl.rvt.gatas.companion.services.BridgeStatus

expect object BridgeBackgroundComponent {
    val status: StateFlow<BridgeStatus>

    fun start(identifier: String)
    fun stop()
}
