package com.faforever.ice.telemetry.ui

import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GpgnetState
import com.faforever.ice.telemetry.domain.IceState
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.ice4j.ice.CandidateType
import java.time.Instant
import java.time.format.DateTimeFormatter

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
interface OutgoingUiMessage {
}

data class UpdateAdapterInfo(
    val gameId: Int,
    val playerId: Int,
    val version: String,
    val protocolVersion: Int,
    val playerName: String,
    val connectedHost: String?,
    val gpgnetState: GpgnetState,
    val gameState: Game.State,
) : OutgoingUiMessage

data class UpdateGame(
    val gameId: Int,
    val hostPlayerId: Int,
    val gameState: Game.State,
    val participants: List<PlayerMeta>,
) : OutgoingUiMessage {
    companion object {
        fun fromGame(game: Game) = UpdateGame(
            game.id.id,
            game.host.id,
            game.state,
            game.participants.map { (_, meta) ->
                UpdateGame.PlayerMeta(
                    meta.player.id.id,
                    meta.player.name,
                    meta.adapter.protocolVersion.id,
                    meta.adapter.version,
                    meta.adapter.connectedHost,
                    meta.adapter.gpgnetState,
                    meta.adapter.gameState,
                    meta.connections.map {
                        PlayerConnection(
                            it.remotePlayer.id.id,
                            it.remotePlayer.name,
                            it.state,
                            it.localCandidate,
                            it.remoteCandidate,
                            it.averageRTT,
                            it.lastReceived
                        )
                    }
                )
            }
        )
    }

    data class PlayerMeta(
        val playerId: Int,
        val playerName: String,
        val protocolVersion: Int,
        val adapterVersion: String,
        val connectedHost: String?,
        val gpgnetState: GpgnetState,
        val gameState: Game.State,
        val connections: List<PlayerConnection>,
    )
}

data class PlayerConnection(
    val playerId: Int,
    val playerName: String,
    val state: IceState,
    val localCandidate: CandidateType?,
    val remoteCandidate: CandidateType?,
    val averageRTT: Double?,
    val lastReceived: Instant?
)

data class UpdateCoturnList(
    val playerId: Int,
    val connectedHost: String,
    val knownServers: List<CoturnServer>
) : OutgoingUiMessage {
    data class CoturnServer(
        val region: String,
        val host: String,
        val port: Int,
        val averageRTT: Double?
    )
}