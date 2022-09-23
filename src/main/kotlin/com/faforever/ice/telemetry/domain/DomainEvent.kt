package com.faforever.ice.telemetry.domain

import com.faforever.ice.telemetry.ProtocolVersion
import com.faforever.ice.telemetry.SessionId

sealed interface DomainEvent {
}

data class PeerConnected(
    val gameId: GameId,
    val adapterVersion: String,
    val protocolVersion: ProtocolVersion,
    val playerId: PlayerId,
    val playerName: String,
    val sessionId: SessionId,
): DomainEvent

data class UiConnected(
    val gameId: GameId,
    val playerId: PlayerId,
    val sessionId: SessionId,
): DomainEvent

data class GameUpdated(val game: Game): DomainEvent

data class ClientRequestCurrentState(val sessionId: SessionId, val adapter: Adapter?, val game: Game): DomainEvent

data class ClientRequestedUnknownGame(val sessionId: SessionId, val gameId: GameId): DomainEvent

data class CoturnServer(
    val region: String,
    val host: String,
    val port: Int,
    val averageRTT: Double
)
data class CoturnListUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val connectedHost: String,
    val knownServers: List<CoturnServer>
) : DomainEvent