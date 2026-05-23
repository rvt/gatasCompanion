package nl.rvt.gatas.companion

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.posix.memcpy

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

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun loadThumbnail(icaoHexCode: String): ByteArray? = withContext(Dispatchers.IO) {
        val path = thumbnailPath(icaoHexCode) ?: return@withContext null
        val data = NSData.create(contentsOfFile = path) ?: return@withContext null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        bytes
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveThumbnail(icaoHexCode: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val path = thumbnailPath(icaoHexCode) ?: return@withContext
        NSFileManager.defaultManager.createDirectoryAtPath(
            thumbnailDirectoryPath() ?: return@withContext,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(path, atomically = true)
        }
        Unit
    }

    private fun thumbnailPath(icaoHexCode: String): String? {
        val cacheDir = thumbnailDirectoryPath() ?: return null
        return "$cacheDir/$icaoHexCode.img"
    }

    private fun thumbnailDirectoryPath(): String? {
        return (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String)
            ?.let { "$it/aircraft-thumbnails" }
    }
}
