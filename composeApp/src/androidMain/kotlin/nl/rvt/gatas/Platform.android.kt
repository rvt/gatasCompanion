package nl.rvt.gatas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.juul.kable.AndroidPeripheral
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

lateinit var appContext: Context
private val bluetoothPlatformLog = Logger.withTag("BluetoothPlatform")


actual suspend fun loadKoins() {

}

@OptIn(ExperimentalApi::class)
actual fun restorePeripheralIfPossible(identifier: String): Peripheral? {
    return runCatching {
        Peripheral(identifier) {}
    }.getOrNull()
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
actual suspend fun requestMtuIfSupported(peripheral: Peripheral) {
    val androidPeripheral = peripheral as? AndroidPeripheral
    if (androidPeripheral == null) {
        bluetoothPlatformLog.w { "Peripheral is not AndroidPeripheral" }
        return
    }

    try {
        androidPeripheral.requestMtu(512)
    } catch (e: Exception) {
        bluetoothPlatformLog.e(e) { "Failed to request MTU" }
    }
}

@Composable
actual fun PlatformKeepScreenOnEffect() {
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

actual suspend fun loadEgm2008Bin(): ByteArray = withContext(Dispatchers.IO) {
    val assetManager = appContext.assets
    assetManager.open("composeResources/gatascompanion.composeapp.generated.resources/files/egm2008.bin").use { it.readBytes() }
}
