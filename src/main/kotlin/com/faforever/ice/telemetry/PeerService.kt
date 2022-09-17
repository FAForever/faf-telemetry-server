package com.faforever.ice.telemetry

import com.faforever.ice.telemetry.protocol.v1.SessionState
import io.micronaut.websocket.WebSocketSession
import jakarta.inject.Singleton

@Singleton
class PeerService {
    private val sessionStates = mutableMapOf<WebSocketSession, SessionState>()

    fun getState(webSocketSession: WebSocketSession) = sessionStates[webSocketSession]
}