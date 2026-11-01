package com.faforever.ice.telemetry.adapter.protocol.v1

import com.faforever.ice.telemetry.domain.IceState
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@MicronautTest
class MessagesTest {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `UpdatePeerState without connected field is parsed`() {
        val json =
            """
            {"messageType":"UpdatePeerState","messageId":"a9fa22c9-a2e2-435c-aa1c-4135461b8816","peerPlayerId":557857,
            "iceState":"AWAITING_CANDIDATES","localCandidate":null,"remoteCandidate":null}
            """.trimIndent()

        val message = objectMapper.readValue(json, IncomingMessageV1::class.java) as UpdatePeerState

        assertEquals(557857, message.peerPlayerId)
        assertEquals(IceState.AWAITING_CANDIDATES, message.iceState)
        assertEquals(false, message.connected)
    }
}
