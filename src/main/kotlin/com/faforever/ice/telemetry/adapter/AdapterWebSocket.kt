@file:Suppress("NAME_SHADOWING")

package com.faforever.ice.telemetry.adapter

import com.faforever.ice.telemetry.adapter.protocol.v1.ConnectToPeer
import com.faforever.ice.telemetry.adapter.protocol.v1.DisconnectFromPeer
import com.faforever.ice.telemetry.adapter.protocol.v1.ErrorCode
import com.faforever.ice.telemetry.adapter.protocol.v1.GeneralError
import com.faforever.ice.telemetry.adapter.protocol.v1.IncomingMessageV1
import com.faforever.ice.telemetry.adapter.protocol.v1.OnlyIdMessage
import com.faforever.ice.telemetry.adapter.protocol.v1.OutgoingMessageV1
import com.faforever.ice.telemetry.adapter.protocol.v1.RegisterAsPeer
import com.faforever.ice.telemetry.adapter.protocol.v1.UpdateCoturnList
import com.faforever.ice.telemetry.adapter.protocol.v1.UpdateGameState
import com.faforever.ice.telemetry.adapter.protocol.v1.UpdateGpgnetState
import com.faforever.ice.telemetry.adapter.protocol.v1.UpdatePeerConnectivity
import com.faforever.ice.telemetry.adapter.protocol.v1.UpdatePeerState
import com.faforever.ice.telemetry.domain.AdapterConnected
import com.faforever.ice.telemetry.domain.CoturnListUpdated
import com.faforever.ice.telemetry.domain.CoturnServer
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.GameStateUpdated
import com.faforever.ice.telemetry.domain.GpgnetStateUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.PeerConnectivityUpdated
import com.faforever.ice.telemetry.domain.PeerDisconnected
import com.faforever.ice.telemetry.domain.PeerStateUpdated
import com.faforever.ice.telemetry.domain.PlayerId
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.event.ApplicationEventPublisher
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

@ServerWebSocket("/adapter/v1/game/{gameId}/player/{playerId}")
class AdapterServerWebSocket(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher<Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val gameSessions: MutableMap<GameId, MutableMap<PlayerId, WebSocketSession>> = ConcurrentHashMap()

    @OnOpen
    fun onOpen(gameId: String, playerId: String, session: WebSocketSession) {
        log.info("Websocket opened session id $session.id [protocol=v1,gameId=$gameId]")

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }

        val playerId = (playerId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { PlayerId(it) }

        gameSessions.getOrPut(gameId) { ConcurrentHashMap() }[playerId] = session
    }

    @OnClose
    fun onClose(gameId: Int, playerId: Int, session: WebSocketSession) {
        log.info("Websocket closed session id $session.id [protocol=v1,gameId=$gameId]")

        val gameId = GameId(gameId)
        val playerId = PlayerId(playerId)

        val currentGameSessions = gameSessions[gameId]
            ?: throw IllegalStateException("Game id $gameId not in game sessions")

        currentGameSessions.remove(playerId)
        if (currentGameSessions.isEmpty()) {
            gameSessions.remove(gameId)
        }
    }

    @OnMessage
    fun onMessage(gameId: Int, playerId: Int, message: String, session: WebSocketSession) {
        log.info("onMessage: $message")

        val gameId = GameId(gameId)
        val playerId = PlayerId(playerId)
        val message = parseMessageOrRespondError(message, session) ?: return

        when (message) {
            is RegisterAsPeer -> {
                applicationEventPublisher.publishEventAsync(
                    AdapterConnected(
                        gameId,
                        message.adapterVersion,
                        ProtocolVersion(1),
                        playerId,
                        message.userName,
                        session.getSessionId(),
                    )
                )
            }

            is UpdateCoturnList -> {
                applicationEventPublisher.publishEventAsync(
                    CoturnListUpdated(
                        gameId,
                        playerId,
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

            is UpdateGameState -> {
                applicationEventPublisher.publishEventAsync(
                    GameStateUpdated(
                        gameId,
                        playerId,
                        message.newState,
                    )
                )
            }

            is UpdateGpgnetState -> {
                applicationEventPublisher.publishEventAsync(
                    GpgnetStateUpdated(
                        gameId,
                        playerId,
                        message.newState,
                    )
                )
            }

            is ConnectToPeer -> {
                applicationEventPublisher.publishEventAsync(
                    PeerConnected(
                        gameId,
                        playerId,
                        PlayerId(message.peerPlayerId),
                        message.peerName,
                        message.localOffer
                    )
                )
            }

            is DisconnectFromPeer -> {
                applicationEventPublisher.publishEventAsync(
                    PeerDisconnected(
                        gameId,
                        playerId,
                        PlayerId(message.peerPlayerId)
                    )
                )
            }

            is UpdatePeerState -> {
                applicationEventPublisher.publishEventAsync(
                    PeerStateUpdated(
                        gameId,
                        playerId,
                        PlayerId(message.peerPlayerId),
                        message.iceState,
                        message.localCandidate,
                        message.remoteCandidate,
                    )
                )
            }

            is UpdatePeerConnectivity -> {
                applicationEventPublisher.publishEventAsync(
                    PeerConnectivityUpdated(
                        gameId,
                        playerId,
                        PlayerId(message.peerPlayerId),
                        message.averageRTT,
                        message.lastReceived,
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
}

fun WebSocketSession.getSessionId() = SessionId(id)