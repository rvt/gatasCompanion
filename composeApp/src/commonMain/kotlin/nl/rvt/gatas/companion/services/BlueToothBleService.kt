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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.rvantwisk.gatas.lib.extensions.MessageType
import nl.rvantwisk.gatas.lib.extensions.deserializeGDL90V1
import nl.rvt.gatas.companion.GaTasDevice
import nl.rvt.gatas.companion.Gdl90BridgeSettings
import nl.rvt.gatas.companion.bluetooth.GATAS_PRIMARY_DEVICE
import nl.rvt.gatas.companion.bluetooth.GATAS_COBS_CHARACTERISTIC
import nl.rvt.gatas.companion.bluetooth.GATAS_RXTX_CHARACTERISTIC
import nl.rvt.gatas.companion.liveactivity.GatasLiveActivityBridge
import nl.rvt.gatas.companion.stuff.toHex
import nl.rvt.gatas.requestMtuIfSupported
import kotlin.uuid.ExperimentalUuidApi

private val log = Logger.withTag(BlueToothBleService::class.simpleName ?: "BlueToothBleService")

@OptIn(ExperimentalUuidApi::class)
class BlueToothBleService constructor(
    private val targetDevice: GaTasDevice,
    private val udpRelayService: GatasUdpRelayService = GatasUdpRelayService(),
    private val gdl90UdpBridgeService: Gdl90UdpBridgeService = Gdl90UdpBridgeService(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(BridgeStatus())
    val status: StateFlow<BridgeStatus> = _status.asStateFlow()

    private val nmeaCharacteristic: Characteristic =
        characteristicOf(GATAS_PRIMARY_DEVICE, GATAS_RXTX_CHARACTERISTIC)

    private val cobsCharacteristic: Characteristic =
        characteristicOf(GATAS_PRIMARY_DEVICE, GATAS_COBS_CHARACTERISTIC)

    private var connectedPeripheral: Peripheral? = null
    private var reconnectJob: Job? = null

    companion object {
        private const val RECONNECT_SCAN_TIMEOUT_MILLIS = 5_000L
        private const val RECONNECT_RETRY_DELAY_MILLIS = 1_000L
        private const val SERVICE_DISCOVERY_TIMEOUT_MILLIS = 5_000L
        private val RELAYED_COBS_MESSAGE_TYPES = setOf(
            MessageType.AIRCRAFT_POSITION_REQUEST_V1.value,
            MessageType.AIRCRAFT_CONFIGURATIONS_V2.value,
        )
    }

    fun start() {
        if (connectedPeripheral != null) {
            return
        }

        GatasLiveActivityBridge.setBridgeRunning(true)

        _status.update {
            it.copy(
                running = true,
                connecting = true,
                bleConnected = false,
                udpHealthy = true,
                activeStream = null,
                availableStreams = null,
                gdl90BridgeEnabled = Gdl90BridgeSettings.isEnabled(),
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
                                gdl90BridgeEnabled = Gdl90BridgeSettings.isEnabled(),
                                lastEvent = "Bluetooth connected, waiting for frames..."
                            )
                        }
                        connectedPeripheral?.state?.filterIsInstance<State.Disconnected>()
                            ?.first()
                        log.d("🔌 Disconnected. Reconnecting in ${RECONNECT_RETRY_DELAY_MILLIS}ms...")
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
                    log.e {
                        "⚠️ Connection attempt failed: ${e.message}. Retrying in ${RECONNECT_RETRY_DELAY_MILLIS}ms..."
                    }
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
                delay(RECONNECT_RETRY_DELAY_MILLIS)
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
            gdl90UdpBridgeService.stop()
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
            GatasLiveActivityBridge.reset()
        }
    }

    private suspend fun connectAndObserve(): Peripheral? {
        val device = findReconnectTarget()

        if (device == null) {
            log.w { "❌ Device not found: ${targetDevice.identifier} (${targetDevice.name})" }
            _status.update {
                it.copy(
                    lastError = "Bluetooth device not found",
                    lastEvent = "Saved Bluetooth ID not found, retrying..."
                )
            }
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
                                    GatasLiveActivityBridge.recordPacket()
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
        maybeBridgeGdl90Frame(label, payload)

        val relayDecision = relayDecision(label, payload)
        if (!relayDecision.shouldRelay) {
            log.i {
                "Skipping $label relay to gatasServer for message type ${relayDecision.messageType}"
            }
            _status.update {
                it.copy(
                    activeStream = label,
                    lastEvent = "Skipped $label relay for unsupported message type ${relayDecision.messageType}"
                )
            }
            return
        }

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
        GatasLiveActivityBridge.recordPacket()
        sendResponse(peripheral, characteristic, label, response)
    }

    private fun relayDecision(label: String, payload: ByteArray): RelayDecision {
        if (label != "COBS") {
            return RelayDecision(shouldRelay = true, messageType = null)
        }

        val type = runCatching {
            nl.rvantwisk.gatas.lib.extensions.CobsByteArray(payload).peekAhead()
        }.getOrElse { error ->
            log.w(error) { "Failed to decode COBS message type; skipping relay to gatasServer" }
            return RelayDecision(shouldRelay = false, messageType = null)
        }

        return RelayDecision(
            shouldRelay = type in RELAYED_COBS_MESSAGE_TYPES,
            messageType = type,
        )
    }

    private suspend fun maybeBridgeGdl90Frame(label: String, payload: ByteArray) {
        if (label != "COBS" || !Gdl90BridgeSettings.isEnabled()) {
            return
        }

        val type = runCatching {
            nl.rvantwisk.gatas.lib.extensions.CobsByteArray(payload).peekAhead()
        }.getOrNull()

        if (type != MessageType.GDL90_V1.value) {
            return
        }

        val gdl90 = runCatching {
            deserializeGDL90V1(payload)
        }.getOrElse { e ->
            log.w(e) { "Failed to decode GDL90 COBS frame" }
            return
        }

        try {
            gdl90UdpBridgeService.send(gdl90)
            _status.update {
                it.copy(
                    serverActivityTick = it.serverActivityTick + 1,
                    gdl90FramesBridged = it.gdl90FramesBridged + 1,
                    gdl90BytesBridged = it.gdl90BytesBridged + gdl90.size,
                    gdl90ActivityTick = it.gdl90ActivityTick + 1,
                    gdl90BridgeEnabled = true,
                    lastEvent = "GDL90 frame sent to localhost:4000 (${gdl90.size} bytes)"
                )
            }
        } catch (e: Exception) {
            log.e(e) { "GDL90 UDP bridge failed" }
            _status.update {
                it.copy(
                    lastError = e.message ?: "GDL90 UDP bridge failed",
                    lastEvent = "GDL90 UDP bridge failed"
                )
            }
        }
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

    private suspend fun observableCharacteristic(
        peripheral: Peripheral,
        expected: Characteristic,
        label: String,
    ): Characteristic? {
        val services = withTimeoutOrNull(SERVICE_DISCOVERY_TIMEOUT_MILLIS) {
            peripheral.services.first { it != null }
        }
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

    private suspend fun findReconnectTarget(): Advertisement? {
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

        return withTimeoutOrNull(RECONNECT_SCAN_TIMEOUT_MILLIS) {
            scanner.advertisements
                .filter { it.identifier.toString() == targetDevice.identifier }
                .first()
        }
    }

    private data class RelayDecision(
        val shouldRelay: Boolean,
        val messageType: Int?,
    )

}
