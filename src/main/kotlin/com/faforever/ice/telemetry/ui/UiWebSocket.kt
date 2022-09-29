package com.faforever.ice.telemetry.ui


import com.faforever.ice.telemetry.GameService
import com.faforever.ice.telemetry.domain.AdapterConnected
import com.faforever.ice.telemetry.domain.CoturnListUpdated
import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.GpgnetState
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@ServerWebSocket("/ui/game/{gameId}/")
class UiWebSocket(
    private val objectMapper: ObjectMapper,
    private val gameService: GameService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val activeListeners: MutableMap<GameId, MutableList<WebSocketSession>> = ConcurrentHashMap()


    @OnOpen
    fun onOpen(gameId: String, session: WebSocketSession) {
        log.info("Ui Websocket opened session id $session.id [gameId=$gameId]")

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }

        activeListeners.getOrPut(gameId) {
            Collections.synchronizedList(mutableListOf())
        }.add(session)

        val game = gameService.getGame(gameId) ?: return
        game.participants.values.forEach { playerConnection ->
            session.sendV1(
                AdapterInfoMessage(
                    gameId.id,
                    playerConnection.player.id.id,
                    playerConnection.adapter.version,
                    playerConnection.adapter.protocolVersion.id,
                    playerConnection.player.name,
                    null,
                    GpgnetState.OFFLINE,
                    Game.State.NONE,
                )
            )

            session.sendV1(
                UpdateCoturnList(
                    playerConnection.player.id.id,
                    playerConnection.adapter.connectedHost ?: "",
                    playerConnection.coturnServers.map {
                        UpdateCoturnList.CoturnServer(
                            it.region,
                            it.host,
                            it.port,
                            it.averageRTT,
                        )
                    }
                )
            )

            val game = gameService.getGame(gameId) ?: return
            session.sendV1(UpdateGame.fromGame(game))
        }
    }

    @OnClose
    fun onClose(gameId: String, session: WebSocketSession) {
        log.info("Ui Websocket closed session id $session.id [gameId=$gameId]")

        val gameId = (gameId.toIntOrNull() ?: return session.close(
            CloseReason(CloseReason.UNSUPPORTED_DATA.code, "Invalid gameId")
        )).let { GameId(it) }

        activeListeners[gameId]?.remove(session)

        val currentGameSessions = activeListeners[gameId]
            ?: throw IllegalStateException("Game id $gameId not in game sessions")
        currentGameSessions.remove(session)
        if (currentGameSessions.isEmpty()) {
            activeListeners.remove(gameId)
        }
    }

    @OnMessage
    fun onMessage(message: String, session: WebSocketSession) {
        log.warn("Unexpected message from Ui Websocket: $message")
    }

    private fun WebSocketSession.sendV1(message: OutgoingUiMessage) {
        val message = objectMapper.writeValueAsString(message)
        log.trace("Sending message: $message")

        sendSync(message)
    }

    @EventListener
    fun onEvent(event: AdapterConnected) =
        activeListeners.getOrDefault(event.gameId, emptyList())
            .forEach { session ->
                session.sendV1(
                    AdapterInfoMessage(
                        event.gameId.id,
                        event.playerId.id,
                        event.adapterVersion,
                        event.protocolVersion.id,
                        event.playerName,
                        null,
                        GpgnetState.OFFLINE,
                        Game.State.NONE,
                    )
                )
            }

    @EventListener
    fun onEvent(event: GameUpdated) =
        activeListeners.getOrDefault(event.game.id, emptyList())
            .forEach { it.sendV1(UpdateGame.fromGame(event.game)) }

    @EventListener
    fun onEvent(event: CoturnListUpdated) =
        activeListeners.getOrDefault(event.gameId, emptyList())
            .forEach { session ->
                session.sendV1(
                    UpdateCoturnList(
                        event.playerId.id,
                        event.connectedHost,
                        event.knownServers.map {
                            UpdateCoturnList.CoturnServer(
                                it.region,
                                it.host,
                                it.port,
                                it.averageRTT
                            )
                        }
                    )
                )
            }
}