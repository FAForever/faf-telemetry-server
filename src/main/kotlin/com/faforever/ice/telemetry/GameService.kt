package com.faforever.ice.telemetry

import com.faforever.ice.telemetry.domain.Adapter
import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.ClientRequestCurrentState
import com.faforever.ice.telemetry.domain.ClientRequestedUnknownGame
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.Player
import com.faforever.ice.telemetry.domain.PlayerId
import com.faforever.ice.telemetry.domain.UiConnected
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class Peer(
    val gameId: GameId,
    val playerId: PlayerId,
    val playerName: String,
)

@Singleton
class GameService(
    private val applicationEventPublisher: ApplicationEventPublisher<Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val activeGames: MutableMap<GameId, Game> = ConcurrentHashMap()
    private val adapters: MutableMap<PlayerId, Adapter> = ConcurrentHashMap()

    init {
        log.info("Instantiating demo game 4711")
        activeGames[GameId(4711)] = Game(
            GameId(4711), Player(PlayerId(5000), "Brutus5000"), "LOBBY", mapOf()
        )
        adapters[PlayerId(5000)] = Adapter(
            "0.1.0-SNAPSHOT",
            ProtocolVersion(1),
            PlayerId(5000),
            "Brutus5000",
        )
    }

    @EventListener
    fun onEvent(peerConnected: PeerConnected) {
        adapters[peerConnected.playerId] = Adapter(
            peerConnected.adapterVersion,
            peerConnected.protocolVersion,
            peerConnected.playerId,
            peerConnected.playerName,
        )

        val peer = Peer(
            peerConnected.gameId, peerConnected.playerId, peerConnected.playerName
        )
        val player = Player(peer.playerId, peer.playerName)

        val game = activeGames.computeIfAbsent(peer.gameId) {
            Game(
                peerConnected.gameId, player, "NEW", mapOf(peer.playerId to peer)
            )
        }

        val updatedParticipants = game.participants + (peer.playerId to peer)
        val updatedGame = game.copy(participants = updatedParticipants)
        activeGames[peer.gameId] = updatedGame

        applicationEventPublisher.publishEventAsync(GameUpdated(updatedGame))
    }

    @EventListener
    fun onEvent(uiConnected: UiConnected) {
        val game = activeGames[uiConnected.gameId] ?: run {
            log.warn("UI connected to non-existing game id ${uiConnected.gameId}")
            applicationEventPublisher.publishEventAsync(
                ClientRequestedUnknownGame(
                    uiConnected.sessionId,
                    uiConnected.gameId
                )
            )
            return
        }

        applicationEventPublisher.publishEventAsync(
            ClientRequestCurrentState(
                uiConnected.sessionId,
                adapters[uiConnected.playerId],
                game
            )
        )
    }
}