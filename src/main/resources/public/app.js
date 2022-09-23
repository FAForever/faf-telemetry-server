import { SlAlert } from "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.0.0-beta.83/dist/shoelace.js"

function setVariable(id, value) {
    document.getElementById(id).innerText = value
}

// Always escape HTML for text arguments!
function escapeHtml(html) {
    const div = document.createElement('div');
    div.textContent = html;
    return div.innerHTML;
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

    document.body.append(alert);
    return alert.toast();
}
// Custom function to emit toast notifications
function peristentError(message) {
    const alert = Object.assign(document.createElement('sl-alert'), {
        variant: 'danger',
        closable: false,
        innerHTML: `
        <sl-icon name="exclamation-octagon" slot="icon"></sl-icon>
        ${escapeHtml(message)}
      `
    });

    document.body.append(alert);
    return alert.toast();
}

const params = new Proxy(new URLSearchParams(window.location.search), {
    get: (searchParams, prop) => searchParams.get(prop),
});

const gameId = params.gameId
const playerId = params.playerId;
const isValid = (!!gameId) && (!!playerId)

console.log(`Launched application with gameId=${gameId}, playerId=${playerId}, isValid=${isValid}`)

if (isValid) {
    const telemetrySocket = new WebSocket(`ws://localhost:8080/ws/v1/game/${gameId}`);
    telemetrySocket.onopen = event => {
        console.log("Telemetry socket opened");

        telemetrySocket.send(JSON.stringify({
            messageId: crypto.randomUUID(),
            messageType: "RegisterAsUi",
            playerId: playerId,
        }));
    }
    telemetrySocket.onclose = event => console.log(`Telemetry socket closed (code=${event.code},reason=${event.reason}`);
    telemetrySocket.onmessage = event => {
        const message = JSON.parse(event.data)

        switch (message.messageType) {
            case "Error":
                console.log(`Error on Websocket: ${JSON.stringify(message)}`);
                return;
            case "AdapterMessage":
                console.log(`AdapterMessage: ${JSON.stringify(message)}`);
                document.body.classList.remove("loading")
                document.body.classList.add("loaded")
                setVariable("adapterVersion", message.version)
                setVariable("adapterProtocol", 'v'+message.protocolVersion)
                setVariable("playerId", message.playerId)
                setVariable("playerName", message.playerName)
                return;
            default:
                console.log(`Unmapped message on Websocket: ${JSON.stringify(message)}`);
                return;
        }


    };
} else {
    peristentError("No game id or player id specified!");
}