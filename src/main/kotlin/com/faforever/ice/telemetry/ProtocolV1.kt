package com.faforever.ice.telemetry

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
interface IncomingMessageV1

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
@JsonSubTypes(
    Type(value = JoinGame::class, name = "joinGame"),
)
interface OutgoingMessageV1

/**
 * The minimal possible message to just extract the messageId
 */
data class OnlyIdMessage(val messageId: String)

data class GeneralError(
    val errorCode: ErrorCode,
    val correlationId: String?,
    val metadata: Map<String, Any>? = null,
    val messageId: UUID = UUID.randomUUID(),
): OutgoingMessageV1

data class ErrorResponse(val messageId: UUID = UUID.randomUUID(), val referredMessage: UUID, val errorCode: String): OutgoingMessageV1

data class JoinGame(val messageId: UUID, val userId: Int, val userName: String) : IncomingMessageV1