package com.faforever.ice.telemetry.domain

import com.faforever.ice.telemetry.ProtocolVersion
import com.faforever.ice.telemetry.SessionId
import org.ice4j.ice.CandidateType
import java.time.Instant

sealed interface DomainEvent {}

data class AdapterConnected(
    val gameId: GameId,
    val adapterVersion: String,
    val protocolVersion: ProtocolVersion,
    val playerId: PlayerId,
    val playerName: String,
    val sessionId: SessionId,
) : DomainEvent


data class GameUpdated(val game: Game) : DomainEvent

data class CoturnServer(
    val region: String, val host: String, val port: Int, val averageRTT: Double?
)

data class CoturnListUpdated(
    val gameId: GameId, val playerId: PlayerId, val connectedHost: String, val knownServers: List<CoturnServer>
) : DomainEvent

data class GameStateUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val newGameState: Game.State,
) : DomainEvent

data class GpgnetStateUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val newGameState: GpgnetState,
) : DomainEvent

data class PeerConnected(
    val gameId: GameId,
    val playerId: PlayerId,
    val peerPlayerId: PlayerId,
    val peerPlayerName: String,
    val localOffer: Boolean
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
    val localCandidate: CandidateType,
    val remoteCandidate: CandidateType,
)

data class PeerConnectivityUpdated(
    val gameId: GameId,
    val playerId: PlayerId,
    val peerPlayerId: PlayerId,
    val averageRTT: Double?,
    val lastReceived: Instant?
)