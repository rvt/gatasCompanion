package nl.rvt.gatas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.juul.kable.AndroidPeripheral
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


actual suspend fun loadKoins() {

}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
actual suspend fun requestMtuIfSupported(peripheral: Peripheral) {
    val androidPeripheral = peripheral as? AndroidPeripheral
    if (androidPeripheral == null) {
        Log.w("BluetoothPlatform", "❌ Peripheral is not AndroidPeripheral")
        return
    }

    try {
        val mtu = androidPeripheral.requestMtu(512)
        Log.d("BluetoothPlatform", "✅ Requested MTU, negotiated value: $mtu")
    } catch (e: Exception) {
        Log.e("BluetoothPlatform", "⚠️ Failed to request MTU: ${e.message}")
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