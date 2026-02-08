# Event-Driven DAG Computation Engine

A **configuration-driven, event-driven computation engine** that models computation as an explicit **Directed Acyclic Graph (DAG)** of Topics and Agents.

The project explores how **strong architectural constraints** (DAG enforcement, explicit dataflow, isolated execution units, and separation of concerns) can significantly improve **reasoning, debuggability, and correctness** in event-driven systems.

---

## Why This Project Exists

Event-driven systems often become difficult to reason about due to:

- Implicit mutable state hidden inside callbacks
- Cyclic dependencies and uncontrolled feedback loops
- Tight coupling between computation, orchestration, and presentation
- Execution behavior that depends on timing and scheduling rather than structure

This project explores an alternative design philosophy:

> **Make the computation graph an explicit, validated, first-class architectural artifact.**

Instead of relying on conventions or discipline, the system enforces structural constraints up-front and pushes complexity to **configuration validation time**, not runtime.

---

## Core Architectural Decisions

### DAG enforced at load time
- Graph topology is derived from configuration and validated using **DFS-based cycle detection**
- Cyclic graphs are rejected **before any execution begins**
- This guarantees termination and prevents feedback loops by construction

### Explicit computation units (Agents)
- Agents implement a narrow computation interface
- Inputs and outputs are explicit via Topics
- No implicit coupling between agents

> Note: Some agents maintain **explicit, local input state** (e.g. fan-in agents).  
> State is intentionally visible and bounded, not hidden in global context.

### Explicit fan-in / fan-out modeling
- Fan-out: Topics broadcast events to multiple downstream Agents
- Fan-in: Agents may wait for multiple required inputs before publishing results
- Dependencies are structural, not temporal

### Configuration-driven composition
- Graph structure is defined declaratively at runtime
- Wiring is completely decoupled from agent implementation
- Agents are instantiated dynamically via reflection

### Strict MVC separation
- **Model**: Topics, Agents, Graph, execution semantics
- **Controller**: REST API for loading config and publishing events
- **View**: Cytoscape.js visualization driven via SSE
- The domain model is unaware of HTTP, JSON, or UI concerns

---

## Core Concepts

### Topics
- Named, stateless pub/sub channels
- Fan-out events synchronously to subscribed Agents
- Do **not** persist message history
- Managed centrally via a shared TopicManager registry

### Agents
- Reactive computation units
- Subscribe to input Topics and publish to output Topics
- Execution is driven purely by events, not direct invocation
- Each Agent instance is uniquely identified by configuration

### Events
- Immutable payloads
- Delivered synchronously at the Topic level
- Cascading execution is structurally bounded by the DAG

---

## Execution Model (High-Level)

1. Load textual configuration
2. Instantiate Agents via reflection
3. Wire Agents and Topics into a bipartite graph
4. Validate acyclicity (fail-fast)
5. Wrap Agents with an execution decorator
6. Publish input events via REST
7. Event-driven cascade across the graph
8. Guaranteed termination due to DAG constraint

---

## Concurrency Model

- Each Agent is wrapped using an **Active Object–style decorator**
- A **dedicated worker thread + bounded queue** serializes execution **per Agent**
- This provides:
  - Isolation between Agents
  - Predictable, per-Agent execution semantics
  - Backpressure via bounded queues

Important clarification:

> **The system does not guarantee a globally deterministic execution order across Agents.**  
> Determinism is structural (DAG correctness, termination) and **per-Agent**, not system-wide scheduling.

This trade-off is intentional and explicit.

---

## Design Patterns in Practice

This project emphasizes **composing patterns to enforce constraints**, not showcasing patterns in isolation.

- **Publish–Subscribe**  
  Topics notify subscribed Agents without coupling.

- **Strategy**  
  Each Agent encapsulates a specific computation behind a common interface.

- **Decorator + Active Object**  
  Execution concerns (threads, queues) are separated from business logic.

- **Factory (Reflection-Based)**  
  Agents are instantiated dynamically from configuration.

- **Singleton**  
  TopicManager enforces a consistent global Topic namespace.

- **Flyweight (Partial)**  
  Topics are shared by name to preserve identity and consistency.

- **Facade**  
  REST controller exposes a simplified interface over complex internal orchestration.

---
## Example Configuration

```text
configs.PlusAgent
A,B
S

configs.IncAgent
S
S1
```

## What This Project Demonstrates

- **Architectural constraint design**  
  Correctness via enforced structure, not convention

- **Explicit dataflow modeling**  
  Computation expressed as a graph, not ad-hoc callbacks

- **Fail-fast validation**  
  Invalid configurations are rejected before execution

- **Isolated execution units**  
  Per-Agent serialized execution with backpressure

- **Clean separation of concerns (MVC)**  
  Domain, control, and presentation layers are isolated

- **Operational visibility**  
  Live event streaming and graph visualization via SSE

---

## Trade-offs & Limitations

- No feedback loops or iterative algorithms (by design)
- No global execution ordering guarantees
- Single-process (JVM-local) runtime
- No persistence or event replay
- At-most-once delivery semantics

These trade-offs intentionally favor **structural correctness, clarity, and debuggability**
over maximal expressiveness or throughput.

---

## Potential Extensions

- Deterministic schedulers / topological execution
- Distributed Topics (Kafka / Redis Streams)
- Explicit stateful Agents with lifecycle management
- Retry policies and dead-letter Topics
- Metrics, tracing, and critical-path analysis
- Static configuration analysis and linting

---

## Intended Audience

This project is intended as:

- A learning tool for **event-driven and dataflow architectures**
- A portfolio demonstration of **system-level architectural thinking**
- A foundation for more advanced **workflow or orchestration engines**

---

## Key Takeaway

> **Constraining a system correctly often matters more than making it more powerful.**

This project demonstrates how explicit structure and validation can dramatically
simplify reasoning about complex, event-driven behavior.
