package nl.rvt.gatas.companion.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.rvt.gatas.companion.services.BlueToothBleService
import nl.rvt.gatas.companion.services.BridgeStatus

class BridgeBackgroundController(
    private val createBridgeService: (String) -> BlueToothBleService = ::BlueToothBleService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(BridgeStatus())
    val status: StateFlow<BridgeStatus> = _status.asStateFlow()

    private var activeIdentifier: String? = null
    private var activeService: BlueToothBleService? = null
    private var statusJob: Job? = null

    fun start(identifier: String) {
        if (activeIdentifier == identifier && activeService != null) {
            return
        }

        stop()

        activeIdentifier = identifier
        val bridgeService = createBridgeService(identifier)
        activeService = bridgeService

        statusJob = scope.launch {
            bridgeService.status.collect { bridgeStatus ->
                _status.value = bridgeStatus
            }
        }

        bridgeService.start()
    }

    fun stop() {
        statusJob?.cancel()
        statusJob = null

        activeService?.stop()
        activeService = null
        activeIdentifier = null
        _status.value = BridgeStatus()
    }
}
