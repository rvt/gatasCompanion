package nl.rvt.gatas.companion.services

/**
 * Applies protocol events to immutable UI state.
 *
 * Keeping these transitions outside the BLE service prevents an unrelated
 * relay success from clearing a GDL90 error and makes each state transition
 * deterministic in unit tests.
 */
object BridgeStatusReducer {
    fun initialGdl90(enabled: Boolean): Gdl90BridgeStatus = Gdl90BridgeStatus(
        enabled = enabled,
        state = if (enabled) Gdl90State.WaitingForFrames else Gdl90State.Disabled,
        lastEvent = if (enabled) {
            "Waiting for GDL90 frames from GATAS"
        } else {
            "GDL90 forwarding disabled"
        },
    )

    fun reduceGdl90(status: BridgeStatus, result: Gdl90ForwardResult): BridgeStatus =
        when (result) {
            Gdl90ForwardResult.Disabled -> status.copy(gdl90 = Gdl90BridgeStatus())
            Gdl90ForwardResult.NotGdl90 -> status
            is Gdl90ForwardResult.Sent -> {
                val current = status.gdl90
                status.copy(
                    gdl90 = current.copy(
                        enabled = true,
                        state = Gdl90State.Sending,
                        framesReceived = current.framesReceived + 1,
                        packetsSent = current.packetsSent + 1,
                        bytesSent = current.bytesSent + result.byteCount,
                        activityTick = current.activityTick + 1,
                        lastEvent = "GDL90 packet sent to this device on UDP 4000",
                        lastError = null,
                    ),
                    lastEvent = "GDL90 packet sent to 127.0.0.1:4000 (${result.byteCount} bytes)",
                )
            }
            is Gdl90ForwardResult.DecodeError -> {
                val current = status.gdl90
                status.copy(gdl90 = current.copy(
                    enabled = true,
                    state = Gdl90State.Error,
                    framesReceived = current.framesReceived + 1,
                    decodeErrors = current.decodeErrors + 1,
                    lastEvent = "Invalid GDL90 frame received",
                    lastError = result.message,
                ))
            }
            is Gdl90ForwardResult.SendError -> {
                val current = status.gdl90
                status.copy(gdl90 = current.copy(
                    enabled = true,
                    state = Gdl90State.Error,
                    framesReceived = current.framesReceived + 1,
                    sendErrors = current.sendErrors + 1,
                    lastEvent = "GDL90 forwarding error",
                    lastError = result.message,
                ))
            }
        }
}
