const timeFormatter = new Intl.RelativeTimeFormat('en', {style: 'long', numeric: 'auto'});
const coturnData = {
    columns: [{
        key: "region",
        text: "Region",
    }, {
        key: "host",
        text: "Host",
    }, {
        key: "port",
        text: "Port",
        textAlign: "center",
    }, {
        key: "averageRtt",
        text: "Average RTT",
        textAlign: "right",
        formatData: (milliseconds) => {
            return milliseconds == null ? "n/a" : milliseconds + ' ms';
        },
    }],
    rows: [{
        region: "Europe",
        host: "coturn-eu-1.supcomhub.org",
        port: 3478
    }, {
        region: "Europe",
        host: "faforever.com",
        port: 3478
    },]
}

const coturnTable = document.getElementById('coturn-table');
coturnTable.columns = coturnData.columns;
coturnTable.rows = coturnData.rows;

const peerData = {
    columns: [{
        key: "id",
        text: "Player ID",
        textAlign: "center",
    }, {
        key: "login",
        text: "Player Name",
    }, {
        key: "connected",
        text: "Connected",
        "variant": "icon",
        textAlign: "center",
    }, {
        key: "localOffer",
        text: "Local Offer",
        "variant": "icon",
        textAlign: "center",
    }, {
        key: "state",
        text: "State",
    }, {
        key: "averageRtt",
        text: "Average RTT",
        textAlign: "right",
        formatData: (milliseconds) => {
            return milliseconds == null ? "n/a" : milliseconds + ' ms';
        }
    }, {
        key: "lastReceived",
        text: "Last Received",
        textAlign: "center",
        formatData: (milliseconds) => {
            if (milliseconds == null) {
                return "n/a";
            }
            return timeFormatter.format(-Math.floor(milliseconds / 100.0) / 10, 'seconds');
        }
    }, {
        key: "localCandidate",
        text: "Local Candidate",
        textAlign: "center",
    }, {
        key: "remoteCandidate",
        text: "Remote Candidate",
        textAlign: "center",
    }],
    rows: [{
        id: "14",
        login: "Brutus5000",
        connected: {"name": "check"},
        localOffer: {"name": "check"},
        state: "completed",
        averageRtt: 31,
        lastReceived: 1553,
        localCandidate: "prflx",
        remoteCandidate: "host",
    }, {
        id: "2345",
        login: "Askaholic",
        connected: {"name": "wifi-unavailable"},
        localOffer: {"name": "check"},
        state: "awaitingCandidates",
        averageRtt: null,
        lastReceived: 15253,
        localCandidate: "stun",
        remoteCandidate: "prflx",
    }, {
        id: "3456",
        login: "Giebmasse",
        connected: {"name": "check"},
        localOffer: {"name": "check"},
        state: "connected",
        averageRtt: 67,
        lastReceived: 753,
        localCandidate: "local",
        remoteCandidate: "host",
    }]
};

const peerTable = document.getElementById('connections-table');
peerTable.columns = peerData.columns;
peerTable.rows = peerData.rows;

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
            messageType: "registerAsUi",
            gameId: gameId,
            playerId: playerId,
        }));
    }
    telemetrySocket.onclose = event => console.log(`Telemetry socket closed (code=${event.code},reason=${event.reason}`);
    telemetrySocket.onmessage = event => {
        const message = JSON.parse(event.data)

        switch (message.messageType) {
            case "error":
                console.log(`Error on Websocket: ${JSON.stringify(message)}`);
                return;
            default:
                console.log(`Unmapped message on Websocket: ${JSON.stringify(message)}`);
                return;
        }


    };
} else {

}