package nl.rvt.gatas.companion

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import nl.rvt.gatas.companion.stuff.defaultHttpClient

@Serializable
data class AircraftPickerEntry(
    val icaoAddress: Long,
    val icaoHexCode: String,
    val registration: String,
    val aircraftType: String,
)

object AircraftLookupService {
    private val client = defaultHttpClient(timeout = 4_000)

    suspend fun loadCachedAircraftPickerEntries(icaoAddressList: List<Long>): List<AircraftPickerEntry> {
        return icaoAddressList
            .distinct()
            .mapNotNull { icaoAddress ->
                AircraftCacheStore.loadEntry(icaoAddress.toIcaoHex())
            }
    }

    suspend fun loadAircraftPickerEntries(icaoAddressList: List<Long>): List<AircraftPickerEntry> = coroutineScope {
        icaoAddressList
            .distinct()
            .map { icaoAddress ->
                async { lookupAircraft(icaoAddress) }
            }
            .awaitAll()
    }

    private suspend fun lookupAircraft(icaoAddress: Long): AircraftPickerEntry {
        val icaoHexCode = icaoAddress.toIcaoHex()
        val cachedEntry = AircraftCacheStore.loadEntry(icaoHexCode)
        val aircraft = lookupAircraftMetadata(icaoHexCode)

        val entry = AircraftPickerEntry(
            icaoAddress = icaoAddress,
            icaoHexCode = icaoHexCode,
            registration = aircraft?.registration ?: cachedEntry?.registration ?: "-",
            aircraftType = aircraft?.type ?: cachedEntry?.aircraftType ?: "-",
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
