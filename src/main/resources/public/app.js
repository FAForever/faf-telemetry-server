/** @class State and rendering logic for the ICE adapter UI */
class IceUI {
    /**
     * @param {number} gameId ID of the game to render
     * @param {number} playerId ID of the player looking at the ui
     */
    constructor(gameId, playerId) {
        this.gameId = gameId
        this.playerId = playerId
        this.playerName = "n/a"
        this.adapterVersion = "n/a"
        this.adapterProtocol = "n/a"
        this.coturnHost = "n/a"
        this.gpgnetState = "n/a"
        this.gameState = "n/a"
        // A participant is a player with an ice adapter connected to the telemetry server
        this.participants = []
        // A player is an active connection known by at least one participant
        this.players = {}
        this.coturnServers = []
    }

    onLoaded() {
        document.body.classList.remove("loading")
        document.body.classList.add("loaded")
    }

    setVariable(id, value, pillVariantSelector) {
        const varElement = document.getElementById(id)
        varElement.innerText = value

        if (pillVariantSelector && varElement.parentElement.tagName === "SL-BADGE") {
            varElement.parentElement.variant = pillVariantSelector(value)
        }
    }

    setAdapterVersion(value) {
        this.adapterVersion = value
        this.setVariable("adapterVersion", this.adapterVersion)
    }

    setAdapterProtocol(value) {
        this.adapterProtocol = value
        this.setVariable("adapterProtocol", this.adapterProtocol)
    }

    setPlayerId(value) {
        this.playerId = playerId
        this.setVariable("playerId", this.playerId)
    }

    setPlayerName(value) {
        this.playerName = value
        this.setVariable("playerName", this.playerName)
    }

    setGpgnetState(value) {
        this.gpgnetState = value

        this.setVariable("gpgnetState", this.gpgnetState, newState => {
            switch (newState) {
                case "OFFLINE":
                    return "danger";
                case "WAITING_FOR_GAME":
                    return "warning";
                case "GAME_CONNECTED":
                    return "success";
            }
        })
    }

    setGameState(value) {
        this.gameState = value

        this.setVariable("gameState", this.gameState, newState => {
            switch (newState) {
                case "NONE":
                    return "danger";
                default:
                    return "primary";
            }
        })
    }

    setCoturnHost(value) {
        this.coturnHost = value == null ? "n/a" : value

        this.setVariable("coturnHost", this.coturnHost)
    }

    setCoturnServers(coturnServers) {
        this.coturnServers = coturnServers

        const coturnTable = document.getElementById("coturn-table")
        const tbody = coturnTable.tBodies[0]

        const rowsToDelete = tbody.rows.length; // keep the header!
        for (let i = 0; i < rowsToDelete; i++) {
            coturnTable.deleteRow(-1)
        }

        if (this.coturnServers) {
            for (let coturnServer of this.coturnServers) {
                const newRow = tbody.insertRow()

                const regionCol = newRow.insertCell(-1)
                regionCol.innerHTML = coturnServer.region

                const hostCol = newRow.insertCell(-1)
                hostCol.innerHTML = coturnServer.host

                const portCol = newRow.insertCell(-1)
                portCol.innerHTML = coturnServer.port
                portCol.className = 'numeric'

                const averageRTTCol = newRow.insertCell(-1)
                averageRTTCol.innerHTML = coturnServer.averageRTT == null ? "n/a" : coturnServer.averageRTT
                averageRTTCol.className = 'numeric'
            }
        }
    }

    updateParticipants(participants) {
        // potential changes are: new participant, removed participant, updated participant
        // thus it's easier to throw away the old state
        this.participants = participants
            .sort((p1, p2) => p1.playerId > p2.playerId)
            .map((p, index) => ({
                    rowIndex: index + 1, ...p
                })
            )

        // uniquely merge all participants and players in the connections (just id and name)
        this.players = [...new Map(
            this.participants.map(p => [p.playerId, {
                playerId: p.playerId,
                playerName: p.playerName,
            }]).concat(
                this.participants.flatMap(p => p.connections).map(c => [c.playerId, {
                    playerId: c.playerId,
                    playerName: c.playerName,
                }])
            )
        ).values()]
            // and sort them by playerId
            .sort((p1, p2) => p1.playerId > p2.playerId)
            // and assign the column id
            .map((player, index) => ({
                columnIndex: index + 1, ...player
            }))

        this.renderParticipantTable()
    }

    renderParticipantTable() {

        const columnOfPlayerId = Object.fromEntries(this.players.map(p => [p.playerId, p.columnIndex]))

        const table = document.getElementById("connection-table")
        table.innerHTML = ""; // delete all rows
        const headerRow = table.insertRow(-1)
        // One more column for the leading column
        for (let i = 0; i <= this.players.length; i++) {
            const cell = headerRow.insertCell(-1);

            if (i > 0) {
                const p = this.players[i - 1];
                headerRow.cells[p.columnIndex].innerHTML = p.playerName
            }
        }

        for (const p of this.participants) {
            if (p.playerId === playerId) {
                this.setVariable("coturnHost", p.connectedHost == null ? "n/a" : p.connectedHost)
            }

            const participantRow = table.insertRow(-1)
            // One more column for the leading column
            for (let i = 0; i <= this.players.length; i++) {
                const cell = participantRow.insertCell(-1);
                if (i > 0) {
                    cell.innerHTML = this.players[i - 1].playerId === p.playerId ? "self" : "n/a"
                }
            }

            participantRow.cells[0].innerHTML = p.playerName
            if (p.connections) {
                for (const con of p.connections) {
                    const columnIndex = columnOfPlayerId[con.playerId]
                    const cardHtml = this.buildConnectionCard(con.state, con.localCandidate, con.remoteCandidate, con.averageRTT, con.lastReceived)
                    participantRow.cells[columnIndex].innerHTML = cardHtml
                }
            }
        }
    }

    buildConnectionCard(iceState, localCandidateType, remoteCandidateType, averageRTT, lastReceived) {
        let lastReceivedText = "n/a"
        let lastReceivedStyle = "neutral"
        if (lastReceived) {
            lastReceived = Math.round(Date.now() - new Date(lastReceived) / 1000.0)

            if (lastReceived < 1) {
                lastReceivedStyle = "success"
            } else if (lastReceived < 5) {
                lastReceivedStyle = "warning"
            } else {
                lastReceivedStyle = "danger"
            }

            lastReceivedText = `<${lastReceived}s ago`
        }

        let averageRttStyle = "neutral"
        let averageRTTText = "n/a"
        if (averageRTT) {

            if (averageRTT < 100) {
                averageRttStyle = "success"
            } else if (averageRTT < 500) {
                averageRttStyle = "warning"
            } else {
                averageRttStyle = "danger"
            }

            averageRTTText = Math.round(averageRTT * 10) / 10.0 + "ms"
        }

        let iceStateColor

        switch (iceState) {
            case "NEW":
            case "GATHERING":
            case "AWAITING_CANDIDATES":
            case "CHECKING":
                iceStateColor = "--sl-color-warning-300"
                break;
            case "CONNECTED":
            case "COMPLETED":
                iceStateColor = "--sl-color-success-300"
                break;
            case "DISCONNECTED":
                iceStateColor = "--sl-color-neutral-300"
                break;
        }

        iceState = iceState.toLowerCase().replace("_", " ")

        return `
    <div>
        <div style="text-align: center;
                    font-size: var(--sl-font-size-small);
                    font-family: var(--sl-font-sans);
                    background-color: var(${iceStateColor});
                    padding: 0 1ch;">
            ${iceState}
        </div>

        <div style="display: flex; align-items: center; justify-content: center">
            <sl-badge variant="primary">${formatCandidateType(localCandidateType)}</sl-badge>
            <sl-icon name="arrow-right"></sl-icon>
            <sl-badge variant="primary">${formatCandidateType(remoteCandidateType)}</sl-badge>
        </div>

        <div style="display: flex; align-items: center; justify-content: center">
            <sl-tooltip content="Average roundtrip time (aka Ping)">
                <sl-badge variant="${averageRttStyle}" pill style="font-size: var(--sl-font-size-2x-small)">
                    <sl-icon name="arrow-repeat"></sl-icon> ${averageRTTText}
                </sl-badge>
            </sl-tooltip>
            <sl-tooltip content="Last message received">
                <sl-badge variant="${lastReceivedStyle}" pill style="font-size: var(--sl-font-size-2x-small)">
                    <sl-icon name="envelope"></sl-icon> ${lastReceivedText}
                </sl-badge>
            </sl-tooltip>
        </div>
    </div>
`
    }

}

function formatCandidateType(name) {
    switch (name) {
        case undefined:
            return "?";
        case "PEER_REFLEXIVE_CANDIDATE":
            return "prflx"
        case "SERVER_REFLEXIVE_CANDIDATE":
            return "srflx"
        case "RELAYED_CANDIDATE":
            return "relay"
        case "HOST_CANDIDATE":
            return "host"
        case "LOCAL_CANDIDATE":
            return "local"
        case "STUN_CANDIDATE":
            return "stun"
    }
}

// Always escape HTML for text arguments!
function escapeHtml(html) {
    const div = document.createElement('div')
    div.textContent = html
    return div.innerHTML
}

// Custom function to emit toast notifications
function notify(message, variant = 'primary', icon = 'info-circle', duration = 3000) {
    const alert = Object.assign(document.createElement('sl-alert'), {
        variant, closable: true, duration: duration, innerHTML: `
        <sl-icon name="${icon}" slot="icon"></sl-icon>
        ${escapeHtml(message)}
      `
    });

    document.body.append(alert)
    return alert.toast()
}

// Custom function to emit toast notifications
function persistentError(message) {
    const alert = Object.assign(document.createElement('sl-alert'), {
        variant: 'danger', closable: false, innerHTML: `
        <sl-icon name="exclamation-octagon" slot="icon"></sl-icon>
        ${escapeHtml(message)}
      `
    })

    document.body.append(alert)
    return alert.toast()
}

const params = new Proxy(new URLSearchParams(window.location.search), {
    get: (searchParams, prop) => searchParams.get(prop),
})

const gameId = parseInt(params.gameId)
const playerId = parseInt(params.playerId)
let wsHost = params.wsHost != undefined ? params.wsHost : location.origin.replace(/^http/, 'ws')
const isValid = (!!gameId) && (!!playerId)

console.log(`Launched application with gameId=${gameId}, playerId=${playerId}, isValid=${isValid}`);

if (isValid) {
    const iceUI = new IceUI(gameId, playerId)

    const telemetrySocket = new WebSocket(`${wsHost}/ui/game/${gameId}`);
    telemetrySocket.onopen = event => {
        console.log("Telemetry socket opened");
    }
    telemetrySocket.onclose = event => console.log(`Telemetry socket closed (code=${event.code},reason=${event.reason}`);
    telemetrySocket.onmessage = event => {
        const message = JSON.parse(event.data)

        console.log(message);
        if (message.playerId > 0 && message.playerId !== playerId) {
            console.log(`Ignoring message for other player`);
            return
        }

        // noinspection JSUnreachableSwitchBranches
        switch (message.messageType) {
            case "Error":
                console.log(`Error on Websocket: ${JSON.stringify(message)}`);
                return
            case "UpdateAdapterInfo":
                iceUI.onLoaded()
                iceUI.setAdapterVersion(message.version)
                iceUI.setAdapterProtocol('v' + message.protocolVersion)
                iceUI.setPlayerId(message.playerId)
                iceUI.setPlayerName(message.playerName)
                iceUI.setGpgnetState(message.gpgnetState)
                iceUI.setGameState(message.gameState)
                iceUI.setCoturnHost(message.connectedHost)

                return
            case "UpdateCoturnList":
                iceUI.setCoturnServers(message.knownServers)

                return
            case "UpdateGame":
                if (message.gameState != undefined) {
                    iceUI.setGameState(message.gameState)
                }

                if (message.participants == undefined) {
                    return
                }

                iceUI.updateParticipants(message.participants)

                return
            case "GameConnectivityUpdate":
                return
            default:
                console.log(`Unmapped message type on Websocket: ${message.messageType}`)
                return
        }


    };
} else {
    persistentError("No game id or player id specified!");
}
