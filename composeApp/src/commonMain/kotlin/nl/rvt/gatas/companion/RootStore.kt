package nl.rvt.gatas.companion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlin.uuid.ExperimentalUuidApi

private val log = Logger.withTag(RootStore::class.simpleName ?: "RootStore")

class RootStore {
    var state: RootState by mutableStateOf(initialState())
        private set

    fun landing() = setState { copy(screen = Screen.Landing, connectTo = null) }
    fun connected(device: GaTasDevice) {
        KnownDeviceStore.saveLastUsedDevice(device)
        setState { copy(screen = Screen.Connected, connectTo = device) }
    }
    fun blueTooth() = setState { copy(screen = Screen.BlueTooth, connectTo = null) }
    fun settings() = setState { copy(screen = Screen.Settings, connectTo = null) }
    fun setGdl90BridgeEnabled(enabled: Boolean) {
        Gdl90BridgeSettings.setEnabled(enabled)
        setState { copy(gdl90BridgeEnabled = enabled) }
    }


    fun deleteItem(device: GaTasDevice) =
        setState {
            val updated = devices.filterNot { it.identifier == device.identifier }.toSet()
            KnownDeviceStore.saveDevices(updated)
            if (KnownDeviceStore.loadLastUsedDevice()?.identifier == device.identifier) {
                KnownDeviceStore.saveLastUsedDevice(null)
            }
            copy(devices = updated)
        }

    fun addItem(add: GaTasDevice) {
        log.i { "Device added: ${add.name}" }

        setState {
            val updated = devices + add
            KnownDeviceStore.saveDevices(updated)
            copy(devices = updated)
        }
    }


    @OptIn(ExperimentalUuidApi::class)
    private fun initialState(): RootState =
        RootState(
            devices = KnownDeviceStore.loadDevices(),
            screen = Screen.Landing,
            airplanesLiveApiKey = "",
            gdl90BridgeEnabled = Gdl90BridgeSettings.isEnabled()
        )

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    data class RootState(
        val devices: Set<GaTasDevice>,
        val screen: Screen,
        val airplanesLiveApiKey: String,
        val gdl90BridgeEnabled: Boolean,
        val connectTo: GaTasDevice? = null
    )

    enum class Screen {
        Landing,
        Connected,
        BlueTooth,
        Settings
    }

    // https://github.com/terrakok/kmp-awesome
}
