package com.faforever.ice.telemetry.domain

import com.faforever.ice.telemetry.Peer
import com.faforever.ice.telemetry.ProtocolVersion
import java.time.Instant

@JvmInline
value class PlayerId(val id: Int)

@JvmInline
value class GameId(val id: Int)

data class Player(val id: PlayerId, val name: String)

data class PlayerConnection(
    val player: Player,
    val adapter: Adapter,
    val coturnServers: List<CoturnServer>,
)


data class Candidate(val networkAdapter: String)

data class PeerConnection(
    val adapterId: Int,
    val createdAt: Instant,
    val state: Unit,
    val offeringPlayer: Player,
    val offeringNetworkMode: Unit,
    val remotePlayer: Player,
    val remotePlayerNetworkMode: Unit,
)

data class Game(
    val id: GameId,
    val host: PlayerId,
    val state: String,
    val participants: MutableMap<PlayerId, PlayerConnection>
)

data class Adapter(
    val version: String,
    val protocolVersion: ProtocolVersion,
    val connectedHost: String?,
)
