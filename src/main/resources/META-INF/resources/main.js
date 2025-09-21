let activityTableServer = null;
let refreshCountdownInterval = 30;
let refreshCountdown = refreshCountdownInterval;
let refreshCountdownPaused = false;

document.addEventListener("DOMContentLoaded", function() {
    const input = document.getElementById('newServerAddress');
    const addBtn = document.getElementById('addServerBtn');

    document.getElementById('refreshCountdownDisplay').innerText = '' + refreshCountdown;

    showSection('servers');

    function updateAddDisabled() {
        const hasValue = input.value.trim().length > 0;
        addBtn.disabled = !hasValue;
    }
    if (input && addBtn) {
        updateAddDisabled();
        input.addEventListener('input', updateAddDisabled);
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                if (input.value.trim()) {
                    addServer(input.value.trim());
                }
            }
        });
    }

    setInterval(countdownStep, 1000);
});

function setRefreshInterval(interval) {
    const val = parseInt(interval, 10);
    if (!isNaN(val)) {
        refreshCountdownInterval = val;
        refreshCountdown = refreshCountdownInterval;
        document.getElementById('refreshCountdownDisplay').innerText = '' + refreshCountdown;
    }
}

function showSection(name) {
    console.log("Showing Section: " + name);
    const servers = document.getElementById('section-servers');
    const activity = document.getElementById('section-activity');
    const btnServers = document.getElementById('showServersBtn');
    const btnActivity = document.getElementById('showActivityBtn');

    if (name === 'activity') {
        setAUnsetB(servers, activity, btnServers, btnActivity);
        reloadServerActivityTable();
    } else {
        setAUnsetB(activity, servers, btnActivity, btnServers);
    }
    function setAUnsetB(a, b, btnA, btnB) {
        if (a) a.classList.add('hidden');
        if (b) b.classList.remove('hidden');
        if (btnA) btnA.setAttribute('aria-selected', 'true');
        if (btnB) btnB.setAttribute('aria-selected', 'false');
    }
}

function countdownStep() {
    if (refreshCountdownPaused) {
        return;
    }
    refreshCountdown--;
    if (refreshCountdown <= 0) {
        reloadServerTable();
        const activitySection = document.getElementById('section-activity');
        const isActivityVisible = activitySection && !activitySection.classList.contains('hidden');
        if (activityTableServer || isActivityVisible) {
            reloadServerActivityTable();
        }
        refreshCountdown = refreshCountdownInterval;
    }
    document.getElementById('refreshCountdownDisplay').innerText = '' + refreshCountdown;
}

function pauseOrResumeRefreshCountdown() {
    refreshCountdownPaused = !refreshCountdownPaused;
    const btn = document.getElementById('toggleRefreshBtn');
    if (refreshCountdownPaused) {
        btn.textContent = 'Resume';
        btn.setAttribute('aria-pressed', 'true');
    } else {
        btn.textContent = 'Pause';
        btn.setAttribute('aria-pressed', 'false');
    }
}

function changeActivityTableServer(server) {
    activityTableServer = server;
    showSection('activity');
    reloadServerActivityTable();
}

function addServer(server) {
    const trimmed = (server || '').trim();
    if (!trimmed) {
        return;
    }
    console.log("Adding Server: " + trimmed);
    fetch('/rest/server', {
        method: 'POST',
        body: trimmed
    }).then(() => {
        document.getElementById('newServerAddress').value = '';
        document.getElementById('addServerBtn').disabled = true;
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
    const limit = 200;
    const serverParam = activityTableServer == null ? '' : `server=${encodeURIComponent(activityTableServer)}`;
    const query = serverParam ? `${serverParam}&limit=${limit}` : `limit=${limit}`;
    fetch(`/serverActivityTable.html?${query}`).then(response => {
        response.text().then(text => {
            document.getElementById('serverActivityTableWrapper').innerHTML = text;
        })
    })
}
