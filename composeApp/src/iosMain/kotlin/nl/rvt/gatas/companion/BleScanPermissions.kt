package nl.rvt.gatas.companion

import dev.icerock.moko.permissions.PermissionsController

actual suspend fun prepareForBleScan(permissionsController: PermissionsController) {
    // iOS does not expose the same runtime permission model as Android for BLE scanning.
    // CoreBluetooth will handle the system prompt when the scanner starts.
}
