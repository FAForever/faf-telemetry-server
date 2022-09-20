package com.faforever.ice.telemetry

import com.faforever.ice.telemetry.domain.GameId
import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

interface MessageHandler {
    fun onOpen(gameId: GameId, session: WebSocketSession)
    fun onClose(gameId: GameId, session: WebSocketSession)
    fun handle(message: String, session: WebSocketSession)
}

@JvmInline
value class SessionId(val id: String)

@JvmInline
value class ProtocolVersion(val id: Int)

@ServerWebSocket("/ws/v{version}/game/{gameId}")
class ServerWebSocket(
    val messageHandlerV1: com.faforever.ice.telemetry.protocol.v1.MessageHandler
) {
    private val log = LoggerFactory.getLogger(javaClass)


    /**
     * For each protocol version we have a dedicated message handler.
     * Message handlers need to register themselves.
     */
    private val messageHandlers = mutableMapOf<ProtocolVersion, MessageHandler>()
    private val sessionHandlers = mutableMapOf<WebSocketSession, MessageHandler>()

    init {
        messageHandlers[ProtocolVersion(1)] = messageHandlerV1
    }

    @OnOpen
    fun onOpen(version: String, gameId: String, session: WebSocketSession) {
        log.info("Websocket opened session id $session.id [protocol=v$version,gameId=$gameId]")

        val version = (version.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid protocol version")
        )).let { ProtocolVersion(it) }

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }

        val messageHandler = messageHandlers[version] ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Unsupported protocol version")
        ).also { log.warn("User tried to connect to unknown protocol version $version") }

        sessionHandlers[session] = messageHandler
        messageHandler.onOpen(gameId, session)
    }

    @OnClose
    fun onClose(version: String, gameId: String, session: WebSocketSession) {
        log.info("Websocket closed session id $session.id [protocol=v$version,gameId=$gameId]")


        val version = version.toIntOrNull() ?: run {
            log.error("Unparseable gameId $gameId")
            return
        }

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }


        val messageHandler = sessionHandlers[session] ?: run {
            log.warn("Session $session.id has no message handler attached (closing any way)")
            return
        }

        messageHandler.onClose(gameId, session)
        sessionHandlers.remove(session)
    }

    @OnMessage
    fun onMessage(message: String, session: WebSocketSession) {
        log.info("onMessage: $message")

        val messageHandler = sessionHandlers[session]
            ?: throw IllegalStateException("No message handler for session $session.id")
        messageHandler.handle(message, session)
    }
}

fun WebSocketSession.getSessionId() = SessionId(id)