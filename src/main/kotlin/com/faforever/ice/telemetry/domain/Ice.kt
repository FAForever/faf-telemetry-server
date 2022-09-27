package com.faforever.ice.telemetry.domain

import com.fasterxml.jackson.annotation.JsonProperty
import org.ice4j.ice.CandidateType
import java.time.Instant

data class PlayerConnection(
    val remotePlayer: Player,
    val state: IceState,
    val localCandidate: CandidateType,
    val remoteCandidate: CandidateType,
    val averageRTT: Double?,
    val lastReceived: Instant?
)

enum class IceState {
    @JsonProperty("new")
    NEW,
    @JsonProperty("gathering")
    GATHERING,
    @JsonProperty("awaitingCandidates")
    AWAITING_CANDIDATES,
    @JsonProperty("checking")
    CHECKING,
    @JsonProperty("connected")
    CONNECTED,
    @JsonProperty("completed")
    COMPLETED,
    @JsonProperty("disconnected")
    DISCONNECTED,
}