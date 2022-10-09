package com.faforever.ice.telemetry.domain

import com.faforever.ice.telemetry.adapter.protocol.v1.ProtocolVersion
import com.faforever.ice.telemetry.adapter.protocol.v1.SessionId
import com.faforever.ice.telemetry.ui.OutgoingUiMessage
import org.ice4j.ice.CandidateType
import java.time.Instant

sealed interface DomainEvent

data class AdapterConnected(
    val gameId: GameId,
    val adapterVersion: String,
    val protocolVersion: ProtocolVersion,
    val playerId: PlayerId,
    val playerName: String,
    val sessionId: SessionId,
) : DomainEvent

data class AdapterInfoUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val adapterVersion: String,
    val protocolVersion: ProtocolVersion,
    val playerName: String,
    val connectedHost: String?,
    val gpgnetState: GpgnetState,
    val gameState: Game.State,
) : OutgoingUiMessage

data class GameUpdated(val game: Game) : DomainEvent

data class CoturnServer(
    val region: String,
    val host: String,
    val port: Int,
    val averageRTT: Double?,
)

data class CoturnListUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val connectedHost: String,
    val knownServers: List<CoturnServer>,
) : DomainEvent

data class GameStateUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val newState: Game.State,
) : DomainEvent

data class GpgnetStateUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val newState: GpgnetState,
) : DomainEvent

data class PeerConnected(
    val gameId: GameId,
    val playerId: PlayerId,
    val peerPlayerId: PlayerId,
    val peerPlayerName: String,
    val localOffer: Boolean,
)

data class PeerDisconnected(
    val gameId: GameId,
    val playerId: PlayerId,
    val peerPlayerId: PlayerId,
)

data class PeerStateUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val peerPlayerId: PlayerId,
    val iceState: IceState,
    val localCandidate: CandidateType?,
    val remoteCandidate: CandidateType?,
)

data class PeerConnectivityUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val peerPlayerId: PlayerId,
    val averageRTT: Double?,
    val lastReceived: Instant?,
)

data class ScheduledConnectivityUpdate(
    val gameId: GameId,
    val connectivityPerPlayer: Map<PlayerId, List<ConnectionState>>,
) {
    data class ConnectionState(
        val remotePlayerId: PlayerId,
        val averageRTT: Double?,
        val lastReceived: Instant?,
    )
}
