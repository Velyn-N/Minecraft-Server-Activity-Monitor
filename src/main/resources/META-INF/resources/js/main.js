let activityTableSchedulerId = null;

document.addEventListener("DOMContentLoaded", function() {
    setInterval(reloadServerTable, 10000);
})

function reloadServerTable() {
    console.log("Loading Server Table");
    fetch('/serverTable.html').then(response => {
        response.text().then(text => {
            document.getElementById('serverTableWrapper').innerHTML = text;
        })
    })
}

function changeActivityTableServer(server) {
    console.log("Changing Activity Table Server to: " + server);
    clearInterval(activityTableSchedulerId);
    activityTableSchedulerId = setInterval(() => loadServerActivity(server), 10000);
    loadServerActivity(server);
}

function loadServerActivity(server) {
    console.log("Loading Server Activity for server: " + server);
    fetch(`/serverActivityTable.html?server=${encodeURIComponent(server)}`).then(response => {
        response.text().then(text => {
            document.getElementById('serverActivityTableWrapper').innerHTML = text;
        })
    })
}
