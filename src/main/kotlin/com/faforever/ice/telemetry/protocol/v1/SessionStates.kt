package com.faforever.ice.telemetry.protocol.v1

data class PeerContext(
        val gameId: Int,
        val userId: Int,
        val userName: Int,
)

sealed interface SessionState {
    data class Connected(val gameId: Int): SessionState
    data class AssignedToGame(val peerContext: PeerContext): SessionState
    object Disconnected: SessionState
}

class SessionStateHandler {
}