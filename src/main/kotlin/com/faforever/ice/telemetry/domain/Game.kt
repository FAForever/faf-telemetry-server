package com.faforever.ice.telemetry.domain

import com.faforever.ice.telemetry.Peer
import java.time.Instant

@JvmInline
value class PlayerId(val id: Int)

@JvmInline
value class GameId(val id: Int)

data class Player(val id: PlayerId, val name: String)

data class Peer(val gameId: GameId, val userId: Int, val userName: String)

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
        val gameId: GameId,
        val host: Player,
        val state: String,
        val participants: Map<Player, Peer>
)
