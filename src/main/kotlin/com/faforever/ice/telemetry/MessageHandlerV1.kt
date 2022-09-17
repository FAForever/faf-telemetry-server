package com.faforever.ice.telemetry

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.websocket.WebSocketSession
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


@Context
@Singleton
class MessageHandlerV1(
    val objectMapper: ObjectMapper,
    serverWebSocket: ServerWebSocket,
) : MessageHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        serverWebSocket.registerMessageHandler(1, this)
    }

    override fun handle(message: String, gameId: Int, session: WebSocketSession) {
        val message = try { objectMapper.readValue(message, IncomingMessageV1::class.java) } catch (e: Exception) {
            logger.error("Ignoring unparseable message: {}", message, e)
            val correlationId = tryParseMessageId(message)
            session.sendV1(GeneralError(errorCode = ErrorCode.UNPARSEABLE_MESSAGE, correlationId = correlationId))
            return
        }

        logger.info(message.toString())
    }

    private fun WebSocketSession.sendV1(message: OutgoingMessageV1) {
        val message = objectMapper.writeValueAsString(message)
        logger.trace("Sending message: {}", message)

        sendSync(message)
    }

    fun tryParseMessageId(message: String): String? = try {
        objectMapper.readValue(message, OnlyIdMessage::class.java).messageId
    } catch (e: Exception) {
        null
    }
}
