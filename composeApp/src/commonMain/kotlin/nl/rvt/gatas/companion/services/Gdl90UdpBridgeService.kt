package nl.rvt.gatas.companion.services

import co.touchlab.kermit.Logger
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ConnectedDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val gdl90UdpLog = Logger.withTag("Gdl90UdpBridge")

class Gdl90UdpBridgeService(
    private val host: String = "localhost",
    private val port: Int = 4000,
) {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val mutex = Mutex()

    private var socket: ConnectedDatagramSocket? = null

    suspend fun send(payload: ByteArray) = mutex.withLock {
        val activeSocket = ensureSocket()

        try {
            activeSocket.send(
                Datagram(
                    packet = ByteReadPacket(payload),
                    address = InetSocketAddress(host, port),
                )
            )
        } catch (e: Exception) {
            socket?.close()
            socket = null
            gdl90UdpLog.e(e) { "GDL90 UDP bridge failed for $host:$port" }
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
        gdl90UdpLog.i { "GDL90 UDP bridge connected to $host:$port" }
        return created
    }
}
