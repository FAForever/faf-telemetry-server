package com.faforever.ice.telemetry

import com.faforever.ice.telemetry.domain.Adapter
import com.faforever.ice.telemetry.domain.Game
import com.faforever.ice.telemetry.domain.GameId
import com.faforever.ice.telemetry.domain.CoturnListUpdated
import com.faforever.ice.telemetry.domain.GameUpdated
import com.faforever.ice.telemetry.domain.PeerConnected
import com.faforever.ice.telemetry.domain.Player
import com.faforever.ice.telemetry.domain.PlayerConnection
import com.faforever.ice.telemetry.domain.PlayerId
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

    init {
        log.info("Instantiating demo game 4711")
        activeGames[GameId(4711)] = Game(
            GameId(4711),
            PlayerId(5000),
            "STARTING",
            mutableMapOf(
                PlayerId(5000) to PlayerConnection(
                    Player(PlayerId(5000), "Brutus5000"),
                    Adapter(
                        "0.1.0-SNAPSHOT", ProtocolVersion(1),
                        "faforever.com"
                    ),
                    listOf()
                )
            )
        )
    }

    fun getGame(gameId: GameId) = activeGames[gameId]


    @EventListener
    fun onEvent(event: PeerConnected) {
        var game: Game? = activeGames[event.gameId]

        if (game == null) {
            game = Game(
                event.gameId,
                event.playerId,
                "STARTING",
                mutableMapOf()
            )

            activeGames[event.gameId] = game
        }

        game.participants[event.playerId] = PlayerConnection(
            Player(event.playerId, event.playerName),
            Adapter(event.adapterVersion, event.protocolVersion, null),
            emptyList()
        )

        applicationEventPublisher.publishEventAsync(GameUpdated(game))
    }

    @EventListener
    fun onEvent(event: CoturnListUpdated) {
        val game = activeGames[event.gameId] ?: return
        val participant = game.participants[event.playerId] ?: return
        game.participants[event.playerId] = participant.copy(coturnServers = event.knownServers)
    }

}