package nl.rvt.gatas.companion

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_CONNECT
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import dev.icerock.moko.permissions.location.COARSE_LOCATION
import dev.icerock.moko.permissions.location.LOCATION

actual suspend fun prepareForBleScan(permissionsController: PermissionsController) {
    permissionsController.providePermission(Permission.BLUETOOTH_SCAN)
    // Older Android versions still gate BLE scanning behind location-style permissions.
    permissionsController.providePermission(Permission.COARSE_LOCATION)
    permissionsController.providePermission(Permission.LOCATION)
    permissionsController.providePermission(Permission.BLUETOOTH_CONNECT)
}
