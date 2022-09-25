package com.faforever.ice.telemetry

import com.faforever.ice.telemetry.adapter.protocol.v1.ErrorCode
import com.faforever.ice.telemetry.adapter.protocol.v1.GameUpdatedMessage
import com.faforever.ice.telemetry.adapter.protocol.v1.GeneralError
import com.faforever.ice.telemetry.adapter.protocol.v1.IncomingMessageV1
import com.faforever.ice.telemetry.adapter.protocol.v1.OnlyIdMessage
import com.faforever.ice.telemetry.adapter.protocol.v1.OutgoingMessageV1
import com.faforever.ice.telemetry.adapter.protocol.v1.RegisterAsPeer
import com.faforever.ice.telemetry.adapter.protocol.v1.UpdateCoturnList
import com.faforever.ice.telemetry.domain.ClientRequestedUnknownGame
import com.faforever.ice.telemetry.domain.CoturnListUpdated
import com.faforever.ice.telemetry.domain.CoturnServer
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.PlayerId
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@JvmInline
value class SessionId(val id: String)

@JvmInline
value class ProtocolVersion(val id: Int)

@ServerWebSocket("/adapter/v1/game/{gameId}")
class AdapterServerWebSocket(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher<Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val gameSessions: MutableMap<GameId, MutableList<WebSocketSession>> = ConcurrentHashMap()
    private val sessionsById: MutableMap<SessionId, WebSocketSession> = ConcurrentHashMap()

    @OnOpen
    fun onOpen(gameId: String, session: WebSocketSession) {
        log.info("Websocket opened session id $session.id [protocol=v1,gameId=$gameId]")

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }

        sessionsById[SessionId(session.id)] = session
        gameSessions.getOrPut(gameId) { mutableListOf() }.add(session)
    }

    @OnClose
    fun onClose(gameId: String, session: WebSocketSession) {
        log.info("Websocket closed session id $session.id [protocol=v1,gameId=$gameId]")

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }

        sessionsById.remove(session.getSessionId())

        val currentGameSessions = gameSessions[gameId]
            ?: throw IllegalStateException("Game id $gameId not in game sessions")
        currentGameSessions.remove(session)
        if (currentGameSessions.isEmpty()) {
            gameSessions.remove(gameId)
        }
    }

    @OnMessage
    fun onMessage(gameId: Int, message: String, session: WebSocketSession) {
        log.info("onMessage: $message")

        val gameId = GameId(gameId)
        val message = parseMessageOrRespondError(message, session) ?: return

        when (message) {
            is RegisterAsPeer -> {
                applicationEventPublisher.publishEventAsync(
                    PeerConnected(
                        gameId,
                        message.adapterVersion,
                        ProtocolVersion(1),
                        PlayerId(message.playerId),
                        message.userName,
                        session.getSessionId(),
                    )
                )
            }

            is UpdateCoturnList -> {
                applicationEventPublisher.publishEventAsync(
                    CoturnListUpdated(
                        gameId,
                        PlayerId(message.playerId),
                        message.connectedHost,
                        message.knownServers.map {
                            CoturnServer(
                                it.region,
                                it.host,
                                it.port,
                                it.averageRTT,
                            )
                        }
                    )
                )
            }
        }

    }

    private fun parseMessageOrRespondError(message: String, session: WebSocketSession): IncomingMessageV1? =
        try {
            objectMapper.readValue(message, IncomingMessageV1::class.java)
        } catch (e: Exception) {
            log.error("Ignoring unparseable message: $message", e)
            val correlationId = tryParseMessageId(message)
            session.sendV1(GeneralError(errorCode = ErrorCode.UNPARSEABLE_MESSAGE, correlationId = correlationId))
            null
        }

    private fun WebSocketSession.sendV1(message: OutgoingMessageV1) {
        val message = objectMapper.writeValueAsString(message)
        log.trace("Sending message: $message")

        sendSync(message)
    }

    private fun tryParseMessageId(message: String): String? = try {
        objectMapper.readValue(message, OnlyIdMessage::class.java).messageId
    } catch (e: Exception) {
        null
    }


    @EventListener
    fun handle(event: GameUpdated) {
        val sessions = gameSessions[event.game.id] ?: emptyList()

        sessions.forEach {
            it.sendV1(
                GameUpdatedMessage(
                    event.game.id.id,
                    event.game.host.id,
                    "someState",
                    mapOf()
                )
            )
        }
    }

    @EventListener
    fun handle(event: ClientRequestedUnknownGame) {
        sessionsById[event.sessionId]?.sendV1(
            GeneralError(
                ErrorCode.GAME_UNKNOWN,
                null,
                mapOf("gameId" to event.gameId)
            )
        )
    }
}

fun WebSocketSession.getSessionId() = SessionId(id)