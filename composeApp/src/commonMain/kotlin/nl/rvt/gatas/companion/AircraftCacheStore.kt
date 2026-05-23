package nl.rvt.gatas.companion

expect object AircraftCacheStore {
    suspend fun loadEntry(icaoHexCode: String): AircraftPickerEntry?
    suspend fun saveEntry(entry: AircraftPickerEntry)

    suspend fun loadThumbnail(icaoHexCode: String): ByteArray?
    suspend fun saveThumbnail(icaoHexCode: String, bytes: ByteArray)
}
