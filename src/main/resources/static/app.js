// app.js - Logic only (no styling)
// MVC View layer: event handling, data updates, class toggles

import { GRAPH_STYLE } from './graph-style.js';

const API = {
    CONFIG_LOAD: '/api/config/load',
    CONFIG_UNLOAD: '/api/config/unload',
    TOPICS: '/api/topics',
    GRAPH: '/api/graph',
    PUBLISH: (topic) => `/api/topics/${topic}/publish`,
    CLEAR: (topic) => `/api/topics/${topic}/clear`,
    EVENTS_STREAM: '/api/events/stream'
};

const MAX_EVENTS = 30;
const HIGHLIGHT_DURATION = 600;
const CLEARED_DURATION = 400;

let cy = null;
let eventSource = null;
const topicValues = new Map();

const $ = (sel) => document.querySelector(sel);

const STEP_MS = 1000;
let q = [];
let draining = false;
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

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
        style: GRAPH_STYLE,
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
            const isTopic = node.kind === 'TOPIC';
            const label = isTopic ? (node.id + '\n—') : node.id;
            elements.push({ data: { id: node.id, kind: node.kind, label: label } });
        }
        for (const edge of data.edges) {
            elements.push({ data: { source: edge.from, target: edge.to } });
        }
        topicValues.clear();

        cy.elements().remove();
        cy.add(elements);
        cy.zoom(1);
        cy.pan({ x: 0, y: 0 });
        cy.layout({ 
            name: 'breadthfirst',
            directed: true,
            spacingFactor: 1.5,
            padding: 50,
            fit: true
        }).run();
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
                li.innerHTML = `<span>${t}</span> <button class="clear-btn" data-topic="${t}">Clear</button>`;
                li.querySelector('.clear-btn').addEventListener('click', (e) => {
                    e.stopPropagation();
                    clearTopic(e.target.dataset.topic);
                });
                list.appendChild(li);
            }
        } else {
            const li = document.createElement('li');
            li.textContent = '(none)';
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

function highlightCleared(nodeId) {
    const node = cy.getElementById(nodeId);
    if (node.length === 0) return;
    node.addClass('cleared');
    setTimeout(() => node.removeClass('cleared'), CLEARED_DURATION);
}

function updateTopicValue(nodeId, value) {
    if (!nodeId.startsWith('T')) return;
    topicValues.set(nodeId, value);
    const node = cy.getElementById(nodeId);
    if (node.length === 0) return;
    const displayValue = value !== null && value !== undefined ? value : '—';
    node.data('label', nodeId + '\n' + displayValue);
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
    if (eventSource) return;

    eventSource = new EventSource(API.EVENTS_STREAM);
    updateSSEStatus(true);
    eventSource.onmessage = (e) => {
        try {
            q.push(JSON.parse(e.data));
            if (!draining) drain();
        } catch (err) {
            console.error('Failed to parse SSE event:', err);
        }
    };

    eventSource.onerror = () => {
        updateSSEStatus(false);
    };
}

async function drain() {
    draining = true;
    while (q.length > 0) {
        const event = q.shift();
        addEventToLog(event);

        // Handle highlighting based on event type
        if (event.type === 'TOPIC_CLEARED') {
            highlightCleared(event.from);
        } else if (event.from) {
            highlightNode(event.from);
        }

        // Update topic value display
        if (event.type === 'TOPIC_PUBLISH' || event.type === 'TOPIC_CLEARED') {
            updateTopicValue(event.from, event.value);
        }
        
        await sleep(STEP_MS);
    }
    draining = false;
}

function disconnectSSE() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
    updateSSEStatus(false);
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
            connectSSE();
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
        disconnectSSE();
        topicValues.clear();
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

    if (!eventSource) {
        showError('Cannot publish: no config loaded');
        return;
    }

    try {
        await postJson(API.PUBLISH(topic), { type: 'double', value: String(value) });
    } catch (err) {
        showError('Publish failed: ' + err.message);
    }
}

async function clearTopic(topic) {
    try {
        const result = await postJson(API.CLEAR(topic), {});
        if (!result.ok) {
            showError('Clear failed: ' + (result.error || 'unknown'));
        }
    } catch (err) {
        showError('Clear topic failed: ' + err.message);
    }
}

function init() {
    initGraph();
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
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
