package nl.rvt.gatas.companion.liveactivity

import platform.Foundation.NSLock

actual object GatasLiveActivityBridge {
    private val lock = NSLock()

    private var bridgeRunning: Boolean = false
    private var pendingPackets: Int = 0
    private var rotationDegreesValue: Int = 0

    actual fun setBridgeRunning(running: Boolean) {
        lock.withLock {
            bridgeRunning = running
            if (!running) {
                pendingPackets = 0
                rotationDegreesValue = 0
            }
        }
    }

    actual fun recordPacket() {
        lock.withLock {
            if (bridgeRunning) {
                pendingPackets += 1
            }
        }
    }

    actual fun consumeRotationTick(): Boolean =
        lock.withLock {
            if (!bridgeRunning || pendingPackets <= 0) {
                return@withLock false
            }

            pendingPackets = 0
            rotationDegreesValue = (rotationDegreesValue + 30) % 360
            true
        }

    actual fun isBridgeRunning(): Boolean =
        lock.withLock { bridgeRunning }

    actual fun rotationDegrees(): Int =
        lock.withLock { rotationDegreesValue }

    actual fun reset() {
        lock.withLock {
            bridgeRunning = false
            pendingPackets = 0
            rotationDegreesValue = 0
        }
    }

    private inline fun <T> NSLock.withLock(block: () -> T): T {
        lock()
        return try {
            block()
        } finally {
            unlock()
        }
    }
}
