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
import nl.rvt.gatas.companion.GaTasDevice
import nl.rvt.gatas.companion.services.BlueToothBleService
import nl.rvt.gatas.companion.services.BridgeStatus

class BridgeBackgroundController(
    private val createBridgeService: (GaTasDevice) -> BlueToothBleService = ::BlueToothBleService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(BridgeStatus())
    val status: StateFlow<BridgeStatus> = _status.asStateFlow()

    private var activeDevice: GaTasDevice? = null
    private var activeService: BlueToothBleService? = null
    private var statusJob: Job? = null

    fun start(device: GaTasDevice) {
        if (activeDevice == device && activeService != null) {
            return
        }

        stop()

        activeDevice = device
        val bridgeService = createBridgeService(device)
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
        activeDevice = null
        _status.value = BridgeStatus()
    }

    fun requestAircraftChange(icaoAddress: Long) {
        activeService?.requestAircraftChange(icaoAddress)
    }
}
