package com.faforever.ice.telemetry

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

interface MessageHandler {
    fun onOpen(gameId: Int, session: WebSocketSession)
    fun onClose(session: WebSocketSession)
    fun handle(message: String, gameId: Int, session: WebSocketSession)
}

@ServerWebSocket("/ws/v{version}/game/{gameId}")
class ServerWebSocket {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * For each protocol version we have a dedicated message handler.
     * Message handlers need to register themselves.
     */
    private val messageHandlers = mutableMapOf<Int, MessageHandler>()
    private val sessionHandlers = mutableMapOf<WebSocketSession, MessageHandler>()

    fun registerMessageHandler(protocolVersion: Int, handler: MessageHandler) {
        messageHandlers[protocolVersion] = handler
    }

    @OnOpen
    fun onOpen(version: String, gameId: String, session: WebSocketSession) {
        logger.info("Websocket opened session id $session.id [protocol=v$version,gameId=$gameId]")

        val version = version.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid protocol version")
        )

        val gameId = gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )

        val messageHandler = messageHandlers[version] ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Unsupported protocol version")
        ).also { logger.warn("User tried to connect to unknown protocol version $version") }

        sessionHandlers[session] = messageHandler
        messageHandler.onOpen(gameId, session)
    }

    @OnClose
    fun onClose(version: String, gameId: String, session: WebSocketSession) {
        logger.info("Websocket closed session id $session.id [protocol=v$version,gameId=$gameId]")

        val messageHandler = sessionHandlers[session] ?: run {
            logger.warn("Session $session.id has no message handler attached (closing any way)")
            return
        }

        messageHandler.onClose(session)
        sessionHandlers.remove(session)
    }

    @OnMessage
    fun onMessage(version: Int, gameId: Int, message: String, session: WebSocketSession) {
        logger.info("onMessage [protocol=v$version,gameId=$gameId]: $message")

        val messageHandler = sessionHandlers[session]
            ?: throw IllegalStateException("No message handler for session $session.id")
        messageHandler.handle(message, gameId, session)
    }
}