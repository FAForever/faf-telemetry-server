package com.faforever.ice.telemetry

import com.faforever.ice.telemetry.domain.DomainEvent
import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.Player
import com.faforever.ice.telemetry.domain.PlayerId
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

data class Peer(
    val gameId: GameId,
    val playerId: PlayerId,
    val playerName: String,
    val eventCallback: (DomainEvent) -> Unit
)

@Singleton
class GameService {
    private val activeGames: MutableMap<GameId, Game> = ConcurrentHashMap()

    @EventListener
    fun onEvent(peerConnected: PeerConnected) {
        val peer = Peer(
            peerConnected.gameId, peerConnected.playerId, peerConnected.playerName, peerConnected.eventCallback
        )
        val player = Player(peer.playerId, peer.playerName)

        val game = activeGames.computeIfAbsent(peer.gameId) {
            Game(
                peerConnected.gameId, player, "NEW", mapOf(player to peer)
            )
        }

        val updatedParticipants = game.participants + (player to peer)
        val updatedGame = game.copy(participants = updatedParticipants)
        activeGames[peer.gameId] = updatedGame

        updatedParticipants.values.forEach { it.eventCallback(GameUpdated(updatedGame)) }
    }
}