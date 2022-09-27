package com.faforever.ice.telemetry.adapter.protocol.v1

import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GpgnetState
import com.faforever.ice.telemetry.domain.PlayerId
import com.faforever.ice.telemetry.ui.AdapterInfoMessage
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
@JsonSubTypes(
    Type(value = RegisterAsPeer::class, name = "RegisterAsPeer"),
    Type(value = ConnectToPeer::class, name = "ConnectToPeer"),
    Type(value = DisconnectFromPeer::class, name = "DisconnectFromPeer"),
    Type(value = PeerConnectivityUpdate::class, name = "PeerConnectivityUpdate"),
    Type(value = UpdateGameState::class, name = "UpdateGameChange"),
    Type(value = UpdateGpgnetState::class, name = "UpdateGpgnetState"),
    Type(value = UpdateCoturnList::class, name = "UpdateCoturnList"),
)
interface IncomingMessageV1 {
    val messageId: UUID
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
@JsonSubTypes(
    Type(value = GeneralError::class, name = "Error"),
    Type(value = ErrorResponse::class, name = "Error"),
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
) : OutgoingMessageV1

data class CoturnServer(
    val region: String,
    val host: String,
    val port: Int,
    val averageRTT: Double?
)

data class UpdateCoturnList(
    override val messageId: UUID,
    val connectedHost: String,
    val knownServers: List<CoturnServer>
) : IncomingMessageV1

data class UpdateGameState(
    override val messageId: UUID,
    val newState: Game.State,
) : IncomingMessageV1

data class UpdateGpgnetState(
    override val messageId: UUID,
    val newState: GpgnetState,
) : IncomingMessageV1

data class RegisterAsPeer(
    override val messageId: UUID,
    val adapterVersion: String,
    val userName: String,
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


