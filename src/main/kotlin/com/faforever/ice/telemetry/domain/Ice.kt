package com.faforever.ice.telemetry.domain

import org.ice4j.ice.CandidateType
import java.time.Instant

data class PlayerConnection(
    val remotePlayer: Player,
    val state: IceState = IceState.NEW,
    val localCandidate: CandidateType? = null,
    val remoteCandidate: CandidateType? = null,
    val averageRTT: Double? = null,
    val lastReceived: Instant? = null
)

@Suppress("unused")
enum class IceState {
    NEW,
    GATHERING,
    AWAITING_CANDIDATES,
    CHECKING,
    CONNECTED,
    COMPLETED,
    DISCONNECTED,
}
