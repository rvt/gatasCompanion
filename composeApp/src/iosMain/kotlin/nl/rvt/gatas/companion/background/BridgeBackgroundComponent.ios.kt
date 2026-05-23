package nl.rvt.gatas.companion.background

import nl.rvt.gatas.companion.GaTasDevice

actual object BridgeBackgroundComponent {
    private val controller = BridgeBackgroundController()

    actual val status = controller.status

    actual fun start(device: GaTasDevice) {
        controller.start(device)
    }

    actual fun stop() {
        controller.stop()
    }

    actual fun requestAircraftChange(icaoAddress: Long) {
        controller.requestAircraftChange(icaoAddress)
    }
}
