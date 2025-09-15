let activityTableServer = null;
let refreshCountdownInterval = 30;
let refreshCountdown = refreshCountdownInterval;

document.addEventListener("DOMContentLoaded", function() {
    setInterval(countdownStep, 1000);
    reloadServerActivityChart();
})

function countdownStep() {
    refreshCountdown--;
    if (refreshCountdown <= 0) {
        reloadServerTable();
        reloadServerActivityChart();
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
    reloadServerActivityChart();
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

function reloadServerActivityChart() {
    console.log("Loading Server Activity Chart for server: " + activityTableServer);
    const serverArg = activityTableServer ? `&server=${encodeURIComponent(activityTableServer)}` : '';
    const url = '/rest/activities?maxDataPoints=30' + serverArg;
    fetch(url).then(response => {
        response.json().then(json => {
            console.log("Building Chart...")
            const byServer = new Map(Object.entries(json || {}));

            byServer.forEach(arr => arr.sort((a, b) => new Date(a.recordCreationTime) - new Date(b.recordCreationTime)));

            const isoSet = new Set();
            byServer.forEach(arr => arr.forEach(r => {
                if (!r || !r.recordCreationTime) return;
                isoSet.add(new Date(r.recordCreationTime).toISOString())
            }));
            const labelsIso = Array.from(isoSet).sort();
            const labels = labelsIso.map(iso => new Date(iso).toLocaleString());
            const indexByIso = new Map(labelsIso.map((iso, i) => [iso, i]));

            const palette = [
                '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#06b6d4',
                '#ec4899', '#84cc16', '#f97316', '#22c55e', '#eab308', '#a855f7'
            ];

            const datasets = [];
            let colorIndex = 0;
            byServer.forEach((arr, server) => {
                const data = new Array(labels.length).fill(null);
                arr.forEach(r => {
                    if (!r || !r.recordCreationTime) return;
                    const iso = new Date(r.recordCreationTime).toISOString();
                    const idx = indexByIso.get(iso);
                    if (idx === undefined) return;
                    data[idx] = r.online ? r.playerCount : null;
                });
                const color = palette[colorIndex++ % palette.length];
                datasets.push({
                    label: server,
                    data,
                    borderColor: color,
                    backgroundColor: color + '33',
                    borderWidth: 2,
                    spanGaps: true,
                    tension: 0.2,
                    pointRadius: 2
                });
            });

            const chartElement = document.getElementById('activityChart');
            if (!chartElement) return;
            if (chartElement.chart && typeof chartElement.chart.destroy === 'function') {
                chartElement.chart.destroy();
            }

            const titleText = activityTableServer ? `Online players: ${activityTableServer}` : 'Online players: All servers';

            const chart = new Chart(chartElement, {
                type: 'line',
                data: {
                    labels,
                    datasets
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'nearest',
                        intersect: false
                    },
                    scales: {
                        x: {
                            display: true,
                            title: { display: true, text: 'Time' }
                        },
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: { display: true, text: 'Online players' },
                            beginAtZero: true,
                            suggestedMax: 20
                        }
                    },
                    plugins: {
                        title: { display: true, text: titleText },
                        legend: { display: true },
                        tooltip: {
                            callbacks: {
                                title: (items) => items && items.length ? items[0].label : '',
                                label: (ctx) => {
                                    const server = ctx.dataset.label;
                                    const value = ctx.parsed.y;
                                    return `${server}: ${value ?? 0} players`;
                                },
                                afterLabel: (ctx) => {
                                    const isoAtIndex = labelsIso[ctx.dataIndex];
                                    const arr = byServer.get(ctx.dataset.label) || [];
                                    const rec = arr.find(r => r && r.recordCreationTime && new Date(r.recordCreationTime).toISOString() === isoAtIndex);
                                    if (!rec) return '';
                                    const drt = rec.dataRetrievalTime ? new Date(rec.dataRetrievalTime).toLocaleString() : 'n/a';
                                    const online = rec.online ? 'online' : 'offline';
                                    const pc = rec.playerCount;
                                    const srv = rec.server;
                                    return `dataRetrievalTime: ${drt}\nstatus: ${online}\nplayerCount: ${pc}`;
                                }
                            }
                        }
                    }
                }
            });

            chartElement.chart = chart;
            console.log("Chart built!");
        })
    }).catch(error => {
        console.error('Error loading activity chart:', error);
    });
}
