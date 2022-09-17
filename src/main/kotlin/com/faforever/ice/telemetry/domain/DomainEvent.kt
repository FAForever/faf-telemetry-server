package com.faforever.ice.telemetry.domain

sealed interface DomainEvent {
}

data class PeerConnected(
    val adapterVersion: String,
    val gameId: GameId,
    val playerId: PlayerId,
    val playerName: String,
    val eventCallback: (DomainEvent) -> Unit
): DomainEvent

data class GameUpdated(val game: Game): DomainEvent
