package nl.rvt.gatas.companion.background

actual object BridgeBackgroundComponent {
    private val controller = BridgeBackgroundController()

    actual val status = controller.status

    actual fun start(identifier: String) {
        controller.start(identifier)
    }

    actual fun stop() {
        controller.stop()
    }
}
