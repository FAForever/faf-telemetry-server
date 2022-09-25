package com.faforever.ice.telemetry.protocol.ui

import com.faforever.ice.telemetry.adapter.protocol.v1.IncomingMessageV1
import com.faforever.ice.telemetry.adapter.protocol.v1.OutgoingMessageV1
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
) : OutgoingUiMessage

data class GameUpdatedMessage(
    val gameId: Int,
    val hostPlayerId: Int,
    val state: String,
    val participants: Map<Int, Peer>,
) : OutgoingUiMessage {
    data class Peer(
        val playerId: PlayerId,
        val playerName: String,
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
        val averageRTT: Double
    )
}