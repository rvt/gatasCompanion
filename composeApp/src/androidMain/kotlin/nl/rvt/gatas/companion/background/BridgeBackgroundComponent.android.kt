package nl.rvt.gatas.companion.background

import nl.rvt.gatas.appContext
import nl.rvt.gatas.companion.GaTasDevice

actual object BridgeBackgroundComponent {
    actual val status = GatasBridgeForegroundService.status

    actual fun start(device: GaTasDevice) {
        GatasBridgeForegroundService.start(appContext, device)
    }

    actual fun stop() {
        GatasBridgeForegroundService.stop(appContext)
    }

    actual fun requestAircraftChange(icaoAddress: Long) {
        GatasBridgeForegroundService.requestAircraftChange(appContext, icaoAddress)
    }
}
