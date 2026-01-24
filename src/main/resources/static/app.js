(function() {
    'use strict';

    const API = {
        CONFIG_LOAD: '/api/config/load',
        CONFIG_UNLOAD: '/api/config/unload',
        TOPICS: '/api/topics',
        GRAPH: '/api/graph',
        PUBLISH: (topic) => `/api/topics/${topic}/publish`,
        EVENTS_STREAM: '/api/events/stream'
    };

    const MAX_EVENTS = 30;
    const HIGHLIGHT_DURATION = 600;

    let cy = null;
    let eventSource = null;

    const $ = (sel) => document.querySelector(sel);
    const $$ = (sel) => document.querySelectorAll(sel);

    async function fetchJson(url) {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }

    async function postJson(url, data) {
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }

    function showError(msg) {
        const banner = $('#error-banner');
        banner.textContent = msg;
        banner.classList.remove('hidden');
        setTimeout(() => banner.classList.add('hidden'), 4000);
    }

    function initGraph() {
        cy = cytoscape({
            container: $('#graph-container'),
            style: [
                {
                    selector: 'node',
                    style: {
                        'label': 'data(id)',
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'font-size': '12px',
                        'font-weight': '600',
                        'color': '#c9d1d9',
                        'text-outline-color': '#0d1117',
                        'text-outline-width': 2,
                        'width': 60,
                        'height': 60
                    }
                },
                {
                    selector: 'node[kind="TOPIC"]',
                    style: {
                        'background-color': '#238636',
                        'border-width': 3,
                        'border-color': '#3fb950',
                        'shape': 'ellipse'
                    }
                },
                {
                    selector: 'node[kind="AGENT"]',
                    style: {
                        'background-color': '#1f6feb',
                        'border-width': 3,
                        'border-color': '#58a6ff',
                        'shape': 'round-rectangle',
                        'width': 80
                    }
                },
                {
                    selector: 'node.active',
                    style: {
                        'border-width': 5,
                        'border-color': '#f0883e',
                        'background-color': '#d29922',
                        'z-index': 999
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 2,
                        'line-color': '#484f58',
                        'target-arrow-color': '#484f58',
                        'target-arrow-shape': 'triangle',
                        'curve-style': 'bezier',
                        'arrow-scale': 1.2
                    }
                },
                {
                    selector: 'edge.active',
                    style: {
                        'line-color': '#f0883e',
                        'target-arrow-color': '#f0883e',
                        'width': 4
                    }
                }
            ],
            layout: { name: 'grid' },
            wheelSensitivity: 0.3
        });
    }

    async function renderGraph() {
        try {
            const data = await fetchJson(API.GRAPH);
            const emptyMsg = $('#empty-msg');

            if (!data.nodes || data.nodes.length === 0) {
                cy.elements().remove();
                emptyMsg.classList.remove('hidden');
                return;
            }

            emptyMsg.classList.add('hidden');

            const elements = [];
            for (const node of data.nodes) {
                elements.push({ data: { id: node.id, kind: node.kind } });
            }
            for (const edge of data.edges) {
                elements.push({ data: { source: edge.from, target: edge.to } });
            }

            cy.elements().remove();
            cy.add(elements);
            cy.layout({ 
                name: 'breadthfirst',
                directed: true,
                spacingFactor: 1.5,
                padding: 50
            }).run();
            cy.fit(undefined, 50);
        } catch (err) {
            showError('Failed to load graph: ' + err.message);
        }
    }

    async function loadTopics() {
        try {
            const data = await fetchJson(API.TOPICS);
            const list = $('#topics-list');
            list.innerHTML = '';
            if (data.topics && data.topics.length > 0) {
                for (const t of data.topics) {
                    const li = document.createElement('li');
                    li.textContent = t;
                    list.appendChild(li);
                }
            } else {
                const li = document.createElement('li');
                li.textContent = '(none)';
                li.style.color = '#8b949e';
                list.appendChild(li);
            }
        } catch (err) {
            showError('Failed to load topics: ' + err.message);
        }
    }

    function highlightNode(nodeId) {
        const node = cy.getElementById(nodeId);
        if (node.length === 0) return;

        node.addClass('active');
        const edges = node.connectedEdges().filter(e => e.source().id() === nodeId);
        edges.addClass('active');

        setTimeout(() => {
            node.removeClass('active');
            edges.removeClass('active');
        }, HIGHLIGHT_DURATION);
    }

    function addEventToLog(event) {
        const log = $('#event-log');
        const li = document.createElement('li');

        const time = new Date(event.ts).toLocaleTimeString();
        const type = event.type || 'UNKNOWN';
        const from = event.from || '-';
        const value = event.value !== null && event.value !== undefined ? event.value : '-';

        li.innerHTML = `<span class="event-time">${time}</span> ` +
            `<span class="event-type">${type}</span> ` +
            `from <span class="event-from">${from}</span> ` +
            `val=<span class="event-value">${value}</span>`;

        log.insertBefore(li, log.firstChild);

        while (log.children.length > MAX_EVENTS) {
            log.removeChild(log.lastChild);
        }

        if (event.from) {
            highlightNode(event.from);
        }
    }

    function updateSSEStatus(connected) {
        const status = $('#sse-status');
        const btn = $('#reconnect-btn');
        if (connected) {
            status.textContent = 'Connected';
            status.className = 'status connected';
            btn.classList.add('hidden');
        } else {
            status.textContent = 'Disconnected';
            status.className = 'status disconnected';
            btn.classList.remove('hidden');
        }
    }

    function connectSSE() {
        if (eventSource) {
            eventSource.close();
        }

        eventSource = new EventSource(API.EVENTS_STREAM);

        eventSource.onopen = () => {
            updateSSEStatus(true);
        };

        eventSource.onmessage = (e) => {
            try {
                const event = JSON.parse(e.data);
                addEventToLog(event);
            } catch (err) {
                console.error('Failed to parse SSE event:', err);
            }
        };

        eventSource.onerror = () => {
            updateSSEStatus(false);
            eventSource.close();
            eventSource = null;
        };
    }

    async function loadConfig() {
        const text = $('#config-text').value.trim();
        if (!text) {
            showError('Config text is empty');
            return;
        }
        try {
            const result = await postJson(API.CONFIG_LOAD, { configText: text });
            if (result.ok) {
                addEventToLog({ ts: Date.now(), type: 'CONFIG_LOADED', from: null, value: null });
                await renderGraph();
                await loadTopics();
            } else {
                showError('Load failed: ' + (result.error || 'unknown'));
            }
        } catch (err) {
            showError('Load config failed: ' + err.message);
        }
    }

    async function unloadConfig() {
        try {
            await postJson(API.CONFIG_UNLOAD, {});
            addEventToLog({ ts: Date.now(), type: 'CONFIG_UNLOADED', from: null, value: null });
            await renderGraph();
            await loadTopics();
        } catch (err) {
            showError('Unload config failed: ' + err.message);
        }
    }

    async function publish(topic, value) {
        if (!topic) {
            showError('Topic is required');
            return;
        }
        try {
            await postJson(API.PUBLISH(topic), { type: 'double', value: String(value) });
        } catch (err) {
            showError('Publish failed: ' + err.message);
        }
    }

    function init() {
        initGraph();
        connectSSE();
        renderGraph();
        loadTopics();

        $('#load-btn').addEventListener('click', loadConfig);
        $('#unload-btn').addEventListener('click', unloadConfig);
        $('#reconnect-btn').addEventListener('click', connectSSE);
        $('#refresh-topics').addEventListener('click', loadTopics);

        $('#publish-btn').addEventListener('click', () => {
            const topic = $('#pub-topic').value.trim();
            const value = $('#pub-value').value.trim();
            publish(topic, value);
        });

        $('#quick-a').addEventListener('click', () => publish('A', '5'));
        $('#quick-b').addEventListener('click', () => publish('B', '8'));
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
