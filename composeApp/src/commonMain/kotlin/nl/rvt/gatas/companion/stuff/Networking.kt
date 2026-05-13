package nl.rvt.gatas.companion.stuff

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


fun defaultHttpClient(timeout: Long = 1500): HttpClient = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = timeout  // 900ms timeout for all requests
        connectTimeoutMillis = 300  // 900ms timeout for initial connection
        socketTimeoutMillis = 900   // 900ms timeout for individual socket operations
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun ByteArray.toHex(): String =
    joinToString(separator = ",") { eachByte -> eachByte.toHexString() }
