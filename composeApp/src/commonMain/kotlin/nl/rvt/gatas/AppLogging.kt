package nl.rvt.gatas

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

private var loggingConfigured = false

fun initializeLogging() {
    if (loggingConfigured) {
        return
    }

    Logger.setMinSeverity(Severity.Warn)
    loggingConfigured = true
}
