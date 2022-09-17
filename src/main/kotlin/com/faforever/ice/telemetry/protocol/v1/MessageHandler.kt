package com.faforever.ice.telemetry.protocol.v1

import com.faforever.ice.telemetry.MessageHandler
import com.faforever.ice.telemetry.ServerWebSocket
import com.faforever.ice.telemetry.domain.DomainEvent
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.PlayerId
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.websocket.WebSocketSession
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


@Context
@Singleton
class MessageHandler(
    private val objectMapper: ObjectMapper,
    private val serverWebSocket: ServerWebSocket,
    private val applicationEventPublisher: ApplicationEventPublisher<Any>
) : MessageHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sessionStates = mutableMapOf<WebSocketSession, SessionState>()

    init {
        serverWebSocket.registerMessageHandler(1, this)
    }

    override fun onOpen(gameId: Int, session: WebSocketSession) {
        sessionStates[session] = SessionState.Connected(gameId)
    }

    override fun onClose(session: WebSocketSession) {
        sessionStates.remove(session)
    }

    override fun handle(message: String, gameId: Int, session: WebSocketSession) {
        val message = parseMessageOrRespondError(message, session) ?: return

        when (message) {
            is RegisterAsPeer -> {
                applicationEventPublisher.publishEventAsync(
                    PeerConnected(
                        message.adapterVersion,
                        GameId(message.gameId),
                        PlayerId(message.playerId),
                        message.userName
                    ) { event -> handle(event, session) }
                )
            }
        }
    }

    private fun handle(event: DomainEvent, session: WebSocketSession) {
        when (event) {
            is GameUpdated -> session.sendV1(
                GameUpdatedMessage(
                    event.game.gameId.id,
                    event.game.host.id.id,
                    "someState",
                    mapOf()
                )
            )
        }
    }

    private fun parseMessageOrRespondError(message: String, session: WebSocketSession): IncomingMessageV1? =
        try {
            objectMapper.readValue(message, IncomingMessageV1::class.java)
        } catch (e: Exception) {
            logger.error("Ignoring unparseable message: $message", e)
            val correlationId = tryParseMessageId(message)
            session.sendV1(GeneralError(errorCode = ErrorCode.UNPARSEABLE_MESSAGE, correlationId = correlationId))
            null
        }

    private

    fun WebSocketSession.sendV1(message: OutgoingMessageV1) {
        val message = objectMapper.writeValueAsString(message)
        logger.trace("Sending message: $message")

        sendSync(message)
    }

    fun tryParseMessageId(message: String): String? = try {
        objectMapper.readValue(message, OnlyIdMessage::class.java).messageId
    } catch (e: Exception) {
        null
    }
}
