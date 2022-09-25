package com.faforever.ice.telemetry.adapter.protocol.v1

import com.faforever.ice.telemetry.domain.GameId

data class PeerContext(
        val gameId: Int,
        val userId: Int,
        val userName: Int,
)

sealed interface SessionState {
    data class Connected(val gameId: GameId): SessionState
    data class AssignedToGame(val peerContext: PeerContext): SessionState
    object Disconnected: SessionState
}

class SessionStateHandler {
}