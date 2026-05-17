package nl.rvt.gatas.companion.services

import co.touchlab.kermit.Logger
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ConnectedDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

private val udpLog = Logger.withTag("GatasUdpRelay")

class GatasUdpRelayService(
    private val host: String = "gatas.vantwisk.nl",
    private val port: Int = 3000,
    private val responseTimeoutMs: Long = 2_500,
) {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val mutex = Mutex()

    private var socket: ConnectedDatagramSocket? = null

    suspend fun relay(payload: ByteArray): ByteArray? = mutex.withLock {
        val activeSocket = ensureSocket()

        try {
            activeSocket.send(
                Datagram(
                    packet = Buffer().also { it.write(payload) },
                    address = InetSocketAddress(host, port),
                )
            )

            val response = withTimeoutOrNull(responseTimeoutMs) {
                activeSocket.receive()
            } ?: run {
                udpLog.w { "Timed out waiting for UDP response from $host:$port" }
                return null
            }

            response.packet.readByteArray()
        } catch (e: Exception) {
            socket?.close()
            socket = null
            udpLog.e(e) { "UDP relay failed for $host:$port" }
            throw e
        }
    }

    suspend fun stop() = mutex.withLock {
        socket?.close()
        socket = null
        selectorManager.close()
    }

    private suspend fun ensureSocket(): ConnectedDatagramSocket {
        val current = socket
        if (current != null) {
            return current
        }

        val created = aSocket(selectorManager).udp().connect(
            remoteAddress = InetSocketAddress(host, port)
        )
        socket = created
        udpLog.i { "UDP bridge connected to $host:$port" }
        return created
    }
}
