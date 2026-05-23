package nl.rvt.gatas

import androidx.compose.runtime.Composable
import com.juul.kable.Peripheral

expect suspend fun requestMtuIfSupported(peripheral: Peripheral)
expect fun restorePeripheralIfPossible(identifier: String): Peripheral?

@Composable
expect fun PlatformKeepScreenOnEffect()

expect suspend fun loadEgm2008Bin(): ByteArray

expect suspend fun loadKoins()
