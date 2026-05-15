package nl.rvt.gatas.companion


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.Home
//import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import nl.rvt.gatas.companion.background.BridgeBackgroundComponent
import nl.rvt.gatas.companion.RootStore.Screen
import nl.rvt.gatas.companion.bluetooth.BlueToothSearchScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootContent(
    modifier: Modifier = Modifier,
    permissionsController: PermissionsController
) {
    val model = remember { RootStore() }
    val state = model.state
    val bridgeStatus by BridgeBackgroundComponent.status.collectAsState()

    // Required to bind permissions lifecycle to the activity
    BindEffect(permissionsController)


//    fun Context.findActivity(): Activity? {
//        var context = this
//        while (context is ContextWrapper) {
//            if (context is Activity) return context
//            context = context.baseContext
//        }
//        return null
//    }

    LaunchedEffect(state.screen, state.connectTo) {
        if (state.screen == Screen.Connected) {
            state.connectTo?.let(BridgeBackgroundComponent::start)
        }
    }

    AppTheme {
        Scaffold(
            bottomBar = {
                if (state.screen != Screen.Connected) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Text("Home") },
                            selected = state.screen == Screen.Landing,
                            onClick = model::landing,
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            icon = { Text("Settings") },
                            selected = state.screen == Screen.Settings,
                            onClick = model::settings,
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) {

            Column {
                Text(
                    text = "GATAS Companion",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                when (state.screen) {
                    Screen.Landing -> LandingScreen(
                        devices = state.devices,
                        deleteItem = model::deleteItem,
                        onItemClicked = model::connected,
                        onScanClicked = model::blueTooth,
                        addBleDevice = {},
                        permissionsController = permissionsController!!
                    )

                    Screen.BlueTooth -> {

                        BlueToothSearchScreen(
                            onClose = { model::landing },
                            onItemClicked = {
                                model.addItem(it)
                                model.landing()
                            },
                        )
                    }

                    Screen.Settings -> SettingsScreen(
                        gdl90BridgeEnabled = state.gdl90BridgeEnabled,
                        onGdl90BridgeEnabledChanged = model::setGdl90BridgeEnabled,
                    )

                    Screen.Connected -> ConnectScreen(
                        bridgeStatus = bridgeStatus,
                        onBack = model::landing,
                        onStopBridge = BridgeBackgroundComponent::stop,
                    )
                }
            }
        }
    }
}
