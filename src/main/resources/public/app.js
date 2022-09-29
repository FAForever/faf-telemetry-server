function setVariable(id, value, pillVariantSelector) {
    const varElement = document.getElementById(id)
    varElement.innerText = value

    if (pillVariantSelector && varElement.parentElement.tagName === "SL-BADGE") {
        varElement.parentElement.variant = pillVariantSelector(value)
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
        variant,
        closable: true,
        duration: duration,
        innerHTML: `
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
        variant: 'danger',
        closable: false,
        innerHTML: `
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
const isValid = (!!gameId) && (!!playerId)

console.log(`Launched application with gameId=${gameId}, playerId=${playerId}, isValid=${isValid}`);

if (isValid) {
    const telemetrySocket = new WebSocket(`ws://localhost:8080/ui/game/${gameId}`);
    telemetrySocket.onopen = event => {
        console.log("Telemetry socket opened");
    }
    telemetrySocket.onclose = event => console.log(`Telemetry socket closed (code=${event.code},reason=${event.reason}`);
    telemetrySocket.onmessage = event => {
        const message = JSON.parse(event.data)

        console.log(message);
        if (message.playerId > 0 && message.playerId !== playerId) {
            console.log(`Ignoring message for other player`);
            return;
        }

        switch (message.messageType) {
            case "Error":
                console.log(`Error on Websocket: ${JSON.stringify(message)}`);
                return;
            case "AdapterInfoMessage":
                console.log(`AdapterMessage: ${JSON.stringify(message)}`);

                document.body.classList.remove("loading")
                document.body.classList.add("loaded")
                setVariable("adapterVersion", message.version)
                setVariable("adapterProtocol", 'v' + message.protocolVersion)
                setVariable("playerId", message.playerId)
                setVariable("playerName", message.playerName)
                setVariable("gpgnetState", message.gpgnetState, value => {
                    switch (value) {
                        case "OFFLINE":
                            return "danger";
                        case "WAITING_FOR_GAME":
                            return "warn";
                        case "GAME_CONNECTED":
                            return "success";
                    }
                })
                setVariable("gameState", message.gameState, value => {
                    switch (value) {
                        case "NONE":
                            return "danger";
                        default:
                            return "primary";
                    }
                })
                setVariable("coturnHost", message.connectedHost == null ? "n/a" : message.connectedHost)
                return;
            case "UpdateCoturnList":

                const coturnTable = document.getElementById("coturn-table")
                const tbody = coturnTable.tBodies[0]

                const rowsToDelete = tbody.rows.length; // keep the header!
                for (let i = 0; i < rowsToDelete; i++) {
                    coturnTable.deleteRow(-1)
                }

                if (message.knownServers) {
                    for (let coturnServer of message.knownServers) {
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
                return;
            case "UpdateGame":
                if (message.gameState != undefined) {
                    setVariable("gameState", message.gameState, value => {
                        switch (value) {
                            case "NONE":
                                return "danger";
                            default:
                                return "primary";
                        }
                    })
                }

                const table = document.getElementById("connection-table")
                table.innerHTML = ""; // delete all rows

                if (message.participants == undefined) {
                    return;
                }

                const participantCount = message.participants.length
                const participants = message.participants.map((p, index) => {
                    return {
                        // row index and column index
                        index: index + 1,
                        ...p
                    }
                })

                const participantsIndexById = Object.fromEntries(participants.map(p => [p.playerId, p.index]))

                const headerRow = table.insertRow(-1)
                // One more column for the leading column
                for (let i = 0; i <= participantCount; i++) {
                    headerRow.insertCell(-1);
                }

                for (const p of participants) {
                    headerRow.cells[p.index].innerHTML = p.playerName

                    const participantRow = table.insertRow(-1)
                    // One more column for the leading column
                    for (let i = 0; i <= participantCount; i++) {
                        participantRow.insertCell(-1);
                    }

                    participantRow.cells[0].innerHTML = p.playerName
                    if (p.connections) {
                        for (const con of p.connections) {
                            const conIndex = participantsIndexById[con.playerId]
                            participantRow.cells[conIndex].innerHTML = `${con.localCandidate} -> ${con.remoteCandidate}`
                        }
                    }

                    participantRow.cells[p.index].innerHTML = "-/-"
                }
                return;
            default:
                console.log(`Unmapped message type on Websocket: ${message.messageType}`)
                return;
        }


    };
} else {
    persistentError("No game id or player id specified!");
}