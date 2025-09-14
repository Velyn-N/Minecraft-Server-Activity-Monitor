let activityTableServer = null;
let refreshCountdownInterval = 30;
let refreshCountdown = refreshCountdownInterval;

document.addEventListener("DOMContentLoaded", function() {
    setInterval(countdownStep, 1000);
})

function countdownStep() {
    refreshCountdown--;
    if (refreshCountdown <= 0) {
        reloadServerTable();
        if (activityTableServer) {
            reloadServerActivityTable();
        }
        refreshCountdown = refreshCountdownInterval;
    }
    document.getElementById('refreshCountdownDisplay').innerText = '' + refreshCountdown;
}

function changeActivityTableServer(server) {
    activityTableServer = server;
    reloadServerActivityTable();
}

function addServer(server) {
    console.log("Adding Server: " + server);
    fetch('/rest/server', {
        method: 'POST',
        body: server
    }).then(() => {
        reloadServerTable();
    })
}

function removeServer(server) {
    if (confirm(`Are you sure you want to remove the server '${server}'?`)) {
        console.log("Removing Server: " + server);
        fetch('/rest/server', {
            method: 'DELETE',
            body: server
        }).then(
            reloadServerTable
        )
    }
}

function reloadServerTable() {
    console.log("Loading Server Table");
    fetch('/serverTable.html').then(response => {
        response.text().then(text => {
            document.getElementById('serverTableWrapper').innerHTML = text;
        })
    })
}

function reloadServerActivityTable() {
    console.log("Loading Server Activity Table for server: " + activityTableServer);
    fetch(`/serverActivityTable.html?server=${encodeURIComponent(activityTableServer)}`).then(response => {
        response.text().then(text => {
            document.getElementById('serverActivityTableWrapper').innerHTML = text;
        })
    })
}
