package com.faforever.ice.telemetry.domain

import com.faforever.ice.telemetry.adapter.protocol.v1.ProtocolVersion

@JvmInline
value class PlayerId(val id: Int)

@JvmInline
value class GameId(val id: Int)

data class Player(val id: PlayerId, val name: String)

data class PlayerMeta(
    val player: Player,
    val adapter: Adapter,
    val coturnServers: List<CoturnServer>,
    val connections: List<PlayerConnection>,
)

data class Game(
    val id: GameId,
    val host: PlayerId,
    val state: State,
    val participants: MutableMap<PlayerId, PlayerMeta>
) {
    @Suppress("unused")
    enum class State {
        NONE,
        IDLE,
        LOBBY,
        LAUNCHING,
        ENDED
    }
}

data class Adapter(
    val version: String,
    val protocolVersion: ProtocolVersion,
    val connectedHost: String?,
    val gpgnetState: GpgnetState,
    val gameState: Game.State,
)

@Suppress("unused")
enum class GpgnetState {
    OFFLINE,
    WAITING_FOR_GAME,
    GAME_CONNECTED
}
