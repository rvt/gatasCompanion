package nl.rvt.gatas.companion

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.rvt.gatas.companion.stuff.defaultHttpClient

@Serializable
data class AircraftPickerEntry(
    val icaoAddress: Long,
    val icaoHexCode: String,
    val registration: String,
    val aircraftType: String,
    val imageUrl: String?,
)

object AircraftLookupService {
    private val client = defaultHttpClient(timeout = 4_000)

    suspend fun loadAircraftPickerEntries(icaoAddressList: List<Long>): List<AircraftPickerEntry> = coroutineScope {
        icaoAddressList
            .distinct()
            .map { icaoAddress ->
                async { lookupAircraft(icaoAddress) }
            }
            .awaitAll()
    }

    suspend fun loadThumbnail(icaoHexCode: String, imageUrl: String): ImageBitmap? {
        return runCatching {
            val bytes = AircraftCacheStore.loadThumbnail(icaoHexCode)
                ?: client.get(imageUrl).body<ByteArray>().also {
                    AircraftCacheStore.saveThumbnail(icaoHexCode, it)
                }
            bytes.decodeToImageBitmap()
        }.getOrNull()
    }

    private suspend fun lookupAircraft(icaoAddress: Long): AircraftPickerEntry {
        val icaoHexCode = icaoAddress.toIcaoHex()
        val cachedEntry = AircraftCacheStore.loadEntry(icaoHexCode)
        val aircraft = lookupAircraftMetadata(icaoHexCode)
        val thumbnail = lookupAircraftThumbnail(icaoHexCode)

        val entry = AircraftPickerEntry(
            icaoAddress = icaoAddress,
            icaoHexCode = icaoHexCode,
            registration = aircraft?.registration ?: cachedEntry?.registration ?: "-",
            aircraftType = aircraft?.type ?: cachedEntry?.aircraftType ?: "-",
            imageUrl = thumbnail?.image ?: cachedEntry?.imageUrl,
        )
        AircraftCacheStore.saveEntry(entry)
        return entry
    }

    private suspend fun lookupAircraftMetadata(icaoHexCode: String): AdsbDbAircraft? {
        return runCatching {
            client.get("https://api.adsbdb.com/v0/aircraft/$icaoHexCode")
                .body<AdsbDbEnvelope>()
                .response
                ?.aircraft
        }.getOrNull()
    }

    private suspend fun lookupAircraftThumbnail(icaoHexCode: String): AirportDataImage? {
        return runCatching {
            client.get("https://airport-data.com/api/ac_thumb.json?m=$icaoHexCode&n=1")
                .body<AirportDataEnvelope>()
                .data
                .firstOrNull()
        }.getOrNull()
    }
}

private fun Long.toIcaoHex(): String = toString(16).uppercase().padStart(6, '0')

@Serializable
private data class AdsbDbEnvelope(
    val response: AdsbDbResponse? = null,
)

@Serializable
private data class AdsbDbResponse(
    val aircraft: AdsbDbAircraft? = null,
)

@Serializable
private data class AdsbDbAircraft(
    val registration: String? = null,
    val type: String? = null,
)

@Serializable
private data class AirportDataEnvelope(
    val data: List<AirportDataImage> = emptyList(),
)

@Serializable
private data class AirportDataImage(
    @SerialName("image")
    val image: String? = null,
)
