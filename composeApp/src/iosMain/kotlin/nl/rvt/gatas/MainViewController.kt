package nl.rvt.gatas

import androidx.compose.ui.window.ComposeUIViewController
import nl.rvt.gatas.companion.App
import dev.icerock.moko.permissions.ios.PermissionsController

fun MainViewController() = ComposeUIViewController {
    initializeLogging()
    val permissionsController = PermissionsController()
    App(permissionsController = permissionsController)
}
