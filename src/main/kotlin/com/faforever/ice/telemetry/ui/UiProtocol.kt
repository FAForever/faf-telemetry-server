package com.faforever.ice.telemetry.ui

import com.faforever.ice.telemetry.adapter.protocol.v1.OutgoingMessageV1
import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GpgnetState
import com.faforever.ice.telemetry.domain.PlayerId
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
interface OutgoingUiMessage {
}

data class AdapterInfoMessage(
    val gameId: Int,
    val playerId: Int,
    val version: String,
    val protocolVersion: Int,
    val playerName: String,
    val connectedHost: String?,
    val gpgnetState: GpgnetState,
    val gameState: Game.State,
) : OutgoingUiMessage

data class GameUpdatedMessage(
    val gameId: Int,
    val hostPlayerId: Int,
    val state: Game.State,
    val participants: Map<Int, PlayerMeta>,
) : OutgoingUiMessage {
    data class PlayerMeta(
        val playerId: Int,
        val playerName: String,
        val protocolVersion: Int,
        val adapterVersion: String,
        val connectedHost: String?,
        val gpgnetState: GpgnetState,
        val gameState: Game.State,
        val connections: List<Any>,
    )
}

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