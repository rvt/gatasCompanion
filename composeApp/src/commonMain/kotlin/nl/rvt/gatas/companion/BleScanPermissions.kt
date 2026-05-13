package nl.rvt.gatas.companion

import dev.icerock.moko.permissions.PermissionsController

expect suspend fun prepareForBleScan(permissionsController: PermissionsController)
