package nl.rvt.gatas.companion.bluetooth

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.juul.kable.Identifier
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging.Level.Warnings
import com.juul.kable.logs.SystemLogEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.rvt.gatas.companion.GaTasDevice
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
val GATAS_PRIMARY_DEVICE: Uuid =
    Uuid.parse("0000ffe0-0000-1000-8000-00805f9b34fb")

@OptIn(ExperimentalUuidApi::class)
val GATAS_RXTX_CHARACTERISTIC: Uuid =
    Uuid.parse("0000ffe1-0000-1000-8000-00805f9b34fb")

@OptIn(ExperimentalUuidApi::class)
val GATAS_COBS_CHARACTERISTIC: Uuid =
    Uuid.parse("0000ffe2-0000-1000-8000-00805f9b34fb")

private val log = Logger.withTag(BleScannerViewModel::class.simpleName ?: "BleScannerViewModel")

class BleScannerViewModel : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null


    companion object {
        @OptIn(ExperimentalUuidApi::class)
        val scanner by lazy {
            Scanner {
                filters {
                    match {
                        services = listOf(GATAS_PRIMARY_DEVICE)
                    }
                }
                logging {
                    engine = SystemLogEngine
                    level = Warnings
                }
            }
        }
    }



    private val _advertisements = MutableStateFlow<List<GaTasDevice>>(emptyList())
    val advertisements: StateFlow<List<GaTasDevice>> = _advertisements

    @OptIn(ExperimentalUuidApi::class)
    fun startScanning() {
        if (scanJob?.isActive == true) return
        _advertisements.value = emptyList()

        scanJob = scope.launch {
            val seenAddresses = mutableSetOf<Identifier>()

            try {
                scanner
                    .advertisements
                    .collect { advertisement ->
                        val hasCharacteristic =
                            advertisement.uuids.contains(GATAS_PRIMARY_DEVICE)
                        log.i { "-------- ${advertisement.name} ${advertisement.uuids} ${advertisement.identifier}" }
                        if (
                            seenAddresses.add(advertisement.identifier) &&
                            hasCharacteristic &&
                            !advertisement.name.isNullOrBlank()
                        ) {
                            val device = GaTasDevice(
                                identifier = advertisement.identifier.toString(),
                                name = advertisement.name ?: "N/A",
                            )
                            _advertisements.value += device
                        }
                    }
            } catch (e: CancellationException) {
                log.e { "Scanner error ${e.message}" }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }


    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }
}
