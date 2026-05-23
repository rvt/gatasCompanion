package nl.rvt.gatas.companion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual object AircraftCacheStore {
    private const val ENTRY_PREFIX = "aircraft_entry_"
    private val json = Json { ignoreUnknownKeys = true }

    actual suspend fun loadEntry(icaoHexCode: String): AircraftPickerEntry? = withContext(Dispatchers.IO) {
        val encoded = NSUserDefaults.standardUserDefaults.stringForKey("$ENTRY_PREFIX$icaoHexCode")
            ?: return@withContext null

        runCatching {
            json.decodeFromString<AircraftPickerEntry>(encoded)
        }.getOrNull()
    }

    actual suspend fun saveEntry(entry: AircraftPickerEntry) = withContext(Dispatchers.IO) {
        NSUserDefaults.standardUserDefaults.setObject(
            json.encodeToString(AircraftPickerEntry.serializer(), entry),
            forKey = "$ENTRY_PREFIX${entry.icaoHexCode}",
        )
    }
}
