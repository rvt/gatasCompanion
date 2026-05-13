package nl.rvt.gatas.companion.background

import nl.rvt.gatas.appContext

actual object BridgeBackgroundComponent {
    actual val status = GatasBridgeForegroundService.status

    actual fun start(identifier: String) {
        GatasBridgeForegroundService.start(appContext, identifier)
    }

    actual fun stop() {
        GatasBridgeForegroundService.stop(appContext)
    }
}
