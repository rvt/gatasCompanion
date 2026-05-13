@file:OptIn(ExperimentalStdlibApi::class)

package nl.rvt.gatas.companion.services

import co.touchlab.kermit.Logger
import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.indicate
import com.juul.kable.notify
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging.Level.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.rvt.gatas.companion.bluetooth.GATAS_PRIMARY_DEVICE
import nl.rvt.gatas.companion.bluetooth.GATAS_COBS_CHARACTERISTIC
import nl.rvt.gatas.companion.bluetooth.GATAS_RXTX_CHARACTERISTIC
import nl.rvt.gatas.companion.stuff.toHex
import nl.rvt.gatas.requestMtuIfSupported
import kotlin.uuid.ExperimentalUuidApi

private val log = Logger.withTag(BlueToothBleService::class.simpleName ?: "BlueToothBleService")

class BlueToothBleService @OptIn(ExperimentalUuidApi::class) constructor(
    private val identifier: String,
    private val udpRelayService: GatasUdpRelayService = GatasUdpRelayService(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(BridgeStatus())
    val status: StateFlow<BridgeStatus> = _status.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    private val nmeaCharacteristic: Characteristic =
        characteristicOf(GATAS_PRIMARY_DEVICE, GATAS_RXTX_CHARACTERISTIC)

    @OptIn(ExperimentalUuidApi::class)
    private val cobsCharacteristic: Characteristic =
        characteristicOf(GATAS_PRIMARY_DEVICE, GATAS_COBS_CHARACTERISTIC)

    private var connectedPeripheral: Peripheral? = null
    private var reconnectJob: Job? = null

    fun start() {
        if (connectedPeripheral != null) {
            return
        }

        _status.update {
            it.copy(
                running = true,
                connecting = true,
                bleConnected = false,
                udpHealthy = true,
                activeStream = null,
                availableStreams = null,
                lastError = null,
                lastEvent = "Searching for GATAS BLE device..."
            )
        }

        reconnectJob = scope.launch {
            while (true) {
                try {
                    connectedPeripheral = connectAndObserve()

                    if (connectedPeripheral != null) {
                        _status.update {
                            it.copy(
                                connecting = false,
                                bleConnected = true,
                                activeStream = null,
                                lastEvent = "Bluetooth connected, waiting for frames..."
                            )
                        }
                        connectedPeripheral?.state?.filterIsInstance<State.Disconnected>()
                            ?.first()
                        log.d("🔌 Disconnected. Reconnecting in 3s...")
                        _status.update {
                            it.copy(
                                bleConnected = false,
                                connecting = true,
                                activeStream = null,
                                availableStreams = null,
                                lastError = "Bluetooth disconnected",
                                lastEvent = "Bluetooth disconnected, reconnecting..."
                            )
                        }
                    }
                    connectedPeripheral?.close()
                    connectedPeripheral = null
                } catch (e: Exception) {
                    log.e { "⚠️ Connection attempt failed: ${e.message}. Retrying in 3s..." }
                    _status.update {
                        it.copy(
                            connecting = true,
                            bleConnected = false,
                            activeStream = null,
                            availableStreams = null,
                            lastError = e.message ?: "Connection attempt failed",
                            lastEvent = "Failed to connect to Bluetooth device"
                        )
                    }
                }
                delay(3000)
            }
        }
    }

    fun stop() {
        log.i { "Ble Service stopped" }
        scope.launch {
            reconnectJob?.cancel()
            reconnectJob = null
            connectedPeripheral?.disconnect()
            connectedPeripheral?.close()
            connectedPeripheral = null
            udpRelayService.stop()
            _status.update {
                it.copy(
                    running = false,
                    connecting = false,
                    bleConnected = false,
                    udpHealthy = false,
                    activeStream = null,
                    availableStreams = null,
                    lastEvent = "Bridge stopped",
                    lastError = null
                )
            }
        }
    }

    private suspend fun connectAndObserve(): Peripheral? {
        val device = getDevice(identifier)
            .onEach { /* update UI */ }
            .take(10_000)
            .firstOrNull()

        if (device == null) {
            log.w { "❌ Device not found: $identifier" }
            return null
        }

        log.i { "📡 Connecting to ${device.name} (${device.identifier})..." }
        val newPeripheral = Peripheral(device) {}
        val connectionScope = newPeripheral.connect()
        requestMtuIfSupported(newPeripheral)

        newPeripheral.state
            .filterIsInstance<State.Connected>()
            .first() // Await connection

        log.i { "✅ Connected to ${device.name}" }

        val nmeaObservable = observableCharacteristic(newPeripheral, nmeaCharacteristic, "NMEA")
        val cobsObservable = observableCharacteristic(newPeripheral, cobsCharacteristic, "COBS")
        _status.update {
            it.copy(
                activeStream = buildList {
                    if (nmeaObservable != null) add("NMEA")
                    if (cobsObservable != null) add("COBS")
                }.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: "No observable stream"
            )
        }
        _status.update {
            it.copy(
                availableStreams = buildList {
                    if (nmeaObservable != null) add("NMEA")
                    if (cobsObservable != null) add("COBS")
                }.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: "No observable stream"
            )
        }

        delay(2000)
        connectionScope.launch {
            observeIncomingNotifications(
                connectionScope = connectionScope,
                peripheral = newPeripheral,
                nmeaObservable = nmeaObservable,
                cobsObservable = cobsObservable,
            )
        }
        return newPeripheral
    }

    private suspend fun observeIncomingNotifications(
        connectionScope: CoroutineScope,
        peripheral: Peripheral,
        nmeaObservable: Characteristic?,
        cobsObservable: Characteristic?,
    ) {
        try {
            if (nmeaObservable != null) {
                connectionScope.launchCharacteristicObserver(peripheral, nmeaObservable, "NMEA")
            } else {
                log.w { "NMEA characteristic is not notifiable; skipping observe()" }
            }

            if (cobsObservable != null) {
                connectionScope.launchCharacteristicObserver(peripheral, cobsObservable, "COBS")
            } else {
                log.w { "COBS characteristic is not notifiable; skipping observe()" }
            }
        } catch (e: Exception) {
            log.e(e) { "⚠️ Error observing notifications" }
        }
    }

    private fun CoroutineScope.launchCharacteristicObserver(
        peripheral: Peripheral,
        characteristic: Characteristic,
        label: String,
    ) {
        launch {
            val frameBuffer = mutableListOf<Byte>()

            try {
                peripheral.observe(characteristic)
                    .collect { chunk ->
                        chunk.forEach { byte ->
                            if (byte == 0.toByte()) {
                                if (frameBuffer.isNotEmpty()) {
                                    val payload = frameBuffer.toByteArray()
                                    frameBuffer.clear()
                                    _status.update {
                                        it.copy(
                                            framesReceived = it.framesReceived + 1,
                                            bytesReceived = it.bytesReceived + payload.size,
                                            activeStream = label,
                                            gatasActivityTick = it.gatasActivityTick + 1,
                                            lastEvent = "$label frame received (${payload.size} bytes)"
                                        )
                                    }
                                    handleFrame(peripheral, characteristic, label, payload)
                                }
                            } else {
                                frameBuffer += byte
                            }
                        }
                    }
            } catch (e: Exception) {
                log.e(e) { "⚠️ Error observing $label notifications" }
            }
        }
    }

    private suspend fun handleFrame(
        peripheral: Peripheral,
        characteristic: Characteristic,
        label: String,
        payload: ByteArray,
    ) {
        log.d { "📩 $label BLE -> UDP ${payload.toHex()}" }
        _status.update {
            it.copy(
                serverActivityTick = it.serverActivityTick + 1,
                lastEvent = "Relaying $label frame to UDP (${payload.size} bytes)"
            )
        }

        val response = try {
            udpRelayService.relay(payload)
        } catch (e: Exception) {
            log.e(e) { "⚠️ UDP relay failed" }
            _status.update {
                it.copy(
                    udpHealthy = false,
                    lastError = e.message ?: "UDP relay failed",
                    lastEvent = "UDP relay failed"
                )
            }
            null
        } ?: return

        if (response.isEmpty()) {
            log.w { "⚠️ UDP response was empty" }
            _status.update {
                it.copy(
                    udpHealthy = false,
                    lastError = "UDP response was empty",
                    lastEvent = "No UDP response"
                )
            }
            return
        }

        _status.update {
            it.copy(
                udpHealthy = true,
                activeStream = label,
                framesRelayed = it.framesRelayed + 1,
                bytesRelayed = it.bytesRelayed + response.size,
                serverActivityTick = it.serverActivityTick + 1,
                lastError = null,
                lastEvent = "UDP response received (${response.size} bytes)"
            )
        }
        sendResponse(peripheral, characteristic, label, response)
    }

    private suspend fun sendResponse(
        peripheral: Peripheral,
        characteristic: Characteristic,
        label: String,
        payload: ByteArray
    ) {
        val framedPayload = if (payload.lastOrNull() == 0.toByte()) {
            payload
        } else {
            payload + 0
        }

        val maxWriteSize = runCatching {
            peripheral.maximumWriteValueLengthForType(WriteType.WithoutResponse)
        }.getOrDefault(framedPayload.size.coerceAtLeast(1))

        framedPayload
            .asList()
            .chunked(maxWriteSize)
            .map { chunk -> chunk.toByteArray() }
            .forEach { chunk ->
                log.i { "📤 UDP -> $label BLE ${chunk.toHex()}" }
                peripheral.write(characteristic, chunk)
            }
        _status.update {
            it.copy(
                gatasActivityTick = it.gatasActivityTick + 1,
                lastEvent = "BLE response sent (${payload.size} bytes)"
            )
        }
    }

    private fun List<Byte>.toByteArray(): ByteArray {
        val result = ByteArray(size)
        forEachIndexed { index, value ->
            result[index] = value
        }
        return result
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observableCharacteristic(
        peripheral: Peripheral,
        expected: Characteristic,
        label: String,
    ): Characteristic? {
        val services = peripheral.services.value
        if (services == null) {
            log.w { "No GATT services available yet for $label" }
            return null
        }

        val service = services.firstOrNull { it.serviceUuid == GATAS_PRIMARY_DEVICE }
        if (service == null) {
            log.w { "No GATAS service found while looking for $label" }
            return null
        }

        val discovered = service.characteristics.firstOrNull {
            it.characteristicUuid == expected.characteristicUuid
        }
        if (discovered == null) {
            log.w {
                "$label characteristic ${expected.characteristicUuid} not found in discovered profile"
            }
            return null
        }

        val properties = discovered.properties
        val canObserve = properties.notify || properties.indicate
        if (!canObserve) {
            log.w {
                "$label characteristic ${expected.characteristicUuid} is not notifiable or indicative; " +
                    "properties=${properties}"
            }
            return null
        }

        log.i {
            "$label characteristic ${expected.characteristicUuid} supports observation; properties=${properties}"
        }
        return expected
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun getDevice(identifier: String): Flow<Advertisement> {
        val scanner = Scanner {
            filters {
                match {
                    services = listOf(GATAS_PRIMARY_DEVICE)
                }
            }
            logging {
                level = Events
            }
        }

        return scanner.advertisements
            .filter { it.identifier.toString() == identifier }
    }

}
