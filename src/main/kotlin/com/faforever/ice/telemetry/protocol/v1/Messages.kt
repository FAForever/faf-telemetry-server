package com.faforever.ice.telemetry.protocol.v1

import com.faforever.ice.telemetry.domain.PlayerId
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
@JsonSubTypes(
    Type(value = RegisterAsPeer::class, name = "registerAsPeer"),
    Type(value = ConnectToPeer::class, name = "connectToPeer"),
    Type(value = DisconnectFromPeer::class, name = "disconnectFromPeer"),
    Type(value = PeerConnectivityUpdate::class, name = "peerConnectivityUpdate"),
    Type(value = GameStateChanged::class, name = "gameStateChanged"),
)
interface IncomingMessageV1 {
    val messageId: UUID
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
@JsonSubTypes(
    Type(value = GeneralError::class, name = "error"),
    Type(value = ErrorResponse::class, name = "error"),
)
interface OutgoingMessageV1 {
    val messageId: UUID
}

/**
 * The minimal possible message to just extract the messageId
 */
data class OnlyIdMessage(val messageId: String)

data class GeneralError(
    val errorCode: ErrorCode,
    val correlationId: String?,
    val metadata: Map<String, Any>? = null,
    override val messageId: UUID = UUID.randomUUID(),
) : OutgoingMessageV1

data class ErrorResponse(
    val referredMessage: UUID,
    val errorCode: String,
    override val messageId: UUID = UUID.randomUUID(),
) :
    OutgoingMessageV1

data class GameUpdatedMessage(
    val gameId: Int,
    val hostPlayerId: Int,
    val state: String,
    val participants: Map<Int, Peer>,
    override val messageId: UUID = UUID.randomUUID(),
): OutgoingMessageV1 {
    data class Peer(
        val playerId: PlayerId,
        val playerName: String,
    )
}

//data class HostGame(val messageId: UUID, val gameId:)
//data class JoinGame(val messageId: UUID, val userId: Int, val userName: String) : IncomingMessageV1


data class RegisterAsPeer(
    override val messageId: UUID,
    val adapterVersion: String,
    val gameId: Int,
    val playerId: Int,
    val userName: String
) : IncomingMessageV1

data class ConnectToPeer(
    override val messageId: UUID,
    val id: Int,
    val login: String,
    val localOffer: Boolean
) : IncomingMessageV1

data class DisconnectFromPeer(
    override val messageId: UUID,
    val id: Int,
) : IncomingMessageV1

data class PeerConnectivityUpdate(
    override val messageId: UUID,
    val id: Int,
) : IncomingMessageV1

data class GameStateChanged(
    override val messageId: UUID,
    val newState: String,
) : IncomingMessageV1