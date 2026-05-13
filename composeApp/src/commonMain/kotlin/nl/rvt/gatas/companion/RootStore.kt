package nl.rvt.gatas.companion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.uuid.ExperimentalUuidApi

class RootStore {
    var state: RootState by mutableStateOf(initialState())
        private set

    fun landing() = setState { copy(screen = Screen.Landing, connectTo = null) }
    fun connected(id: String) = setState { copy(screen = Screen.Connected, connectTo = id) }
    fun blueTooth() = setState { copy(screen = Screen.BlueTooth, connectTo = null) }


    fun deleteItem(device: GaTasDevice) =
        setState {
            copy(devices = devices.filterNot { it.identifier == device.identifier }.toSet())
        }

    fun addItem(add: GaTasDevice) {
        println("Device added add.name: ${add.name}")

        setState {
            copy(devices = devices + add)
        }
    }


    @OptIn(ExperimentalUuidApi::class)
    private fun initialState(): RootState =
        RootState(
            devices = setOf(),
            screen = Screen.Landing,
            airplanesLiveApiKey = ""
        )

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    data class RootState(
        val devices: Set<GaTasDevice>,
        val screen: Screen,
        val airplanesLiveApiKey: String,
        val connectTo: String? = null
    )

    enum class Screen {
        Landing,
        Connected,
        BlueTooth
    }

    // https://github.com/terrakok/kmp-awesome
}
