package com.faforever.ice.telemetry.protocol.v1

import com.faforever.ice.telemetry.MessageHandler
import com.faforever.ice.telemetry.ProtocolVersion
import com.faforever.ice.telemetry.SessionId
import com.faforever.ice.telemetry.domain.ClientRequestCurrentState
import com.faforever.ice.telemetry.domain.ClientRequestedUnknownGame
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.PlayerId
import com.faforever.ice.telemetry.domain.UiConnected
import com.faforever.ice.telemetry.getSessionId
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.websocket.WebSocketSession
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap


@Singleton
class MessageHandler(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher<Any>
) : MessageHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val gameSessions: MutableMap<GameId, MutableList<WebSocketSession>> = ConcurrentHashMap()
    private val sessionsById: MutableMap<SessionId, WebSocketSession> = ConcurrentHashMap()

    override fun onOpen(gameId: GameId, session: WebSocketSession) {
        sessionsById[SessionId(session.id)] = session
        gameSessions.getOrPut(gameId) { mutableListOf() }.add(session)
    }

    override fun onClose(gameId: GameId, session: WebSocketSession) {
        sessionsById.remove(session.getSessionId())

        val currentGameSessions = gameSessions[gameId]
            ?: throw IllegalStateException("Game id $gameId not in game sessions")
        currentGameSessions.remove(session)
        if (currentGameSessions.isEmpty()) {
            gameSessions.remove(gameId)
        }
    }

    override fun handle(message: String, session: WebSocketSession) {
        val message = parseMessageOrRespondError(message, session) ?: return

        when (message) {
            is RegisterAsPeer -> {
                applicationEventPublisher.publishEventAsync(
                    PeerConnected(
                        GameId(message.gameId),
                        message.adapterVersion,
                        ProtocolVersion(1),
                        PlayerId(message.playerId),
                        message.userName,
                        session.getSessionId(),
                    )
                )
            }

            is RegisterAsUi -> {
                applicationEventPublisher.publishEventAsync(
                    UiConnected(
                        GameId(message.gameId),
                        PlayerId(message.playerId),
                        session.getSessionId(),
                    )
                )
            }
        }
    }

    @EventListener
    fun handle(event: GameUpdated) {
        val sessions = gameSessions[event.game.id] ?: emptyList()

        sessions.forEach {
            it.sendV1(
                GameUpdatedMessage(
                    event.game.id.id,
                    event.game.host.id.id,
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

    @EventListener
    fun handle(event: ClientRequestCurrentState) {
        val session = sessionsById[event.sessionId] ?: return

        if (event.adapter != null) {
            session.sendV1(
                AdapterMessage(
                    event.adapter.version,
                    event.adapter.protocolVersion.id,
                    event.adapter.playerId.id,
                    event.adapter.playerName,
                )
            )
        }

        session.sendV1(
            GameUpdatedMessage(
                event.game.id.id,
                event.game.host.id.id,
                "someState",
                mapOf()
            )
        )
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

    private fun WebSocketSession.sendV1(message: OutgoingMessageV1) {
        val message = objectMapper.writeValueAsString(message)
        logger.trace("Sending message: $message")

        sendSync(message)
    }

    private fun tryParseMessageId(message: String): String? = try {
        objectMapper.readValue(message, OnlyIdMessage::class.java).messageId
    } catch (e: Exception) {
        null
    }
}
