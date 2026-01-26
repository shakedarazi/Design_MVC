// graph-style.js
// Cytoscape stylesheet configuration - NO runtime logic
// All visual styling for graph nodes and edges

export const GRAPH_STYLE = [
    // Base node style
    {
        selector: 'node',
        style: {
            'label': 'data(label)',
            'text-valign': 'center',
            'text-halign': 'center',
            'font-size': '19px',    
            'font-weight': '600',
            'color': '#ffffff',
            'text-outline-color': '#000000',
            'text-outline-width': 2,
            'text-wrap': 'wrap',
            'text-max-width': '80px',
            'width': 65,
            'height': 65,
            'transition-property': 'border-color, border-width, background-color',
            'transition-duration': '0.2s'
        }
    },
    // Topic nodes - dark gray ellipse
    {
        selector: 'node[kind="TOPIC"]',
        style: {
            'background-color': '#4b5563',
            'background-opacity': 0.9,
            'border-width': 3,
            'border-color': '#6b7280',
            'shape': 'ellipse',
            'width': 80,   // ADD: larger width
            'height': 80 
        }
    },
    // Agent nodes - blue rounded rectangle
    {
        selector: 'node[kind="AGENT"]',
        style: {
            'background-color': '#1f6feb',
            'background-opacity': 0.9,
            'border-width': 3,
            'border-color': '#58a6ff',
            'shape': 'round-rectangle',
            'width': 100,
            'height': 60
        }
    },
    // MulAgent - blue
    {
        selector: 'node[label="MulAgent"]',
        style: {
            'background-color': '#1f6feb',
            'border-color': '#58a6ff'
        }
    },
    // PlusAgent - yellow
    {
        selector: 'node[label="PlusAgent"]',
        style: {
            'background-color': '#ffd700',
            'border-color': '#ffed4e'
        }
    },
    // IncAgent - green
    {
        selector: 'node[label="IncAgent"]',
        style: {
            'background-color': '#238636',
            'border-color': '#3fb950',
            'color': '#ffffff'  // white text on green background
        }
    },
    // DecAgent - red
    {
        selector: 'node[label="DecAgent"]',
        style: {
            'background-color': '#dc3545',
            'border-color': '#ff6b7a'
        }
    },
    // Highlight: active (TOPIC_PUBLISH, AGENT_PUBLISH)
    {
        selector: 'node.active',
        style: {
            'border-width': 6,
            'border-color': '#f0883e',
            'background-color': '#d29922',
            'background-opacity': 1,
            'z-index': 999
        }
    },
    // Highlight: cleared (TOPIC_CLEARED)
    {
        selector: 'node.cleared',
        style: {
            'background-color': '#484f58',
            'border-color': '#6e7681',
            'background-opacity': 0.6,
            'z-index': 999
        }
    },
    // Default edges
    {
        selector: 'edge',
        style: {
            'width': 2,
            'line-color': '#484f58',
            'target-arrow-color': '#484f58',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
            'arrow-scale': 1.3,
            'transition-property': 'line-color, width, target-arrow-color',
            'transition-duration': '0.2s'
        }
    },
    // Highlighted edges (flow effect)
    {
        selector: 'edge.active',
        style: {
            'line-color': '#f0883e',
            'target-arrow-color': '#f0883e',
            'width': 5,
            'z-index': 999
        }
    }
];
