package nl.rvt.gatas.companion

import platform.Foundation.NSUserDefaults

actual object Gdl90BridgeSettings {
    private const val ENABLED_KEY = "gdl90_bridge_enabled"

    actual fun isEnabled(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(ENABLED_KEY)

    actual fun setEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, ENABLED_KEY)
    }
}
