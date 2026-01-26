# Event-Driven DAG Computation Engine

A deterministic, configuration-driven computation engine that models computation as an event-driven **Directed Acyclic Graph (DAG)**.

This project demonstrates how **architectural constraints** (DAG enforcement, stateless agents, strict separation of concerns) can be used to build predictable, debuggable, and extensible systems.

---

## Why This Project Exists

Event-driven systems often become hard to reason about due to:

- Hidden mutable state
- Cyclic dependencies and feedback loops
- Tight coupling between logic, execution, and presentation
- Non-deterministic execution paths

This project explores an alternative design:

> **Treat the computation graph itself as a first-class architectural constraint.**

By enforcing a DAG and keeping computation units stateless, the system guarantees deterministic execution and termination.

---

## Core Architectural Decisions

- **DAG enforced at load time**  
  Graph configurations are validated using DFS; cyclic graphs are rejected before execution.

- **Stateless computation units (Agents)**  
  Agents contain no hidden or persistent state, enabling deterministic replay and easier testing.

- **Explicit fan-in / fan-out modeling**  
  Agents may wait for multiple inputs (fan-in) or broadcast to multiple downstream consumers (fan-out).

- **Configuration-driven composition**  
  Graph topology is defined declaratively at runtime; behavior is decoupled from wiring.

- **Strict MVC separation**  
  The domain model is completely unaware of HTTP, JSON, or UI concerns.

---

## Core Concepts

### Topics
- Named, stateless communication channels
- Fan-out messages to subscribed Agents
- Do not store message history
- Managed centrally via a TopicManager (shared registry)

### Agents
- Stateless processors reacting to incoming events
- Subscribe to input Topics and publish to output Topics
- Execute logic only when all required inputs are available
- Identified deterministically based on configuration (not memory identity)

### Events
- Immutable messages
- Propagate synchronously within a Topic
- Cascading execution completes before control returns to the publisher

---

## Execution Model (High-Level)

1. Load configuration file
2. Validate graph is a DAG (cycle detection via DFS)
3. Instantiate Agents using reflection
4. Wrap Agents with a concurrency decorator
5. Publish input events
6. Deterministic cascade of computations
7. Guaranteed termination due to DAG constraint

---

## Design Patterns in Practice

This project focuses on **pattern composition**, not pattern collection.

- **Publish–Subscribe**  
  Topics notify subscribed Agents without knowing their concrete types.

- **Strategy**  
  Each Agent implements a shared interface while encapsulating a specific computation strategy.

- **Decorator + Active Object**  
  Agents are wrapped to execute asynchronously without modifying their core logic.

- **Factory (Reflection-Based)**  
  Agents are instantiated dynamically from configuration files at runtime.

- **Singleton**  
  A single TopicManager instance enforces a consistent global Topic namespace.

- **Flyweight (Partial)**  
  Topics are shared by name to preserve identity and consistency.

- **Facade**  
  A REST controller exposes a simplified API over complex graph construction and execution.

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

This configuration defines a simple computation chain:

- Inputs **A** and **B** are summed by `PlusAgent`
- The result is published to Topic **S`
- `IncAgent` consumes **S**, increments the value, and publishes **S1**
- Each step is triggered purely by events, without direct coupling between components

## What This Project Demonstrates

- **Architectural constraint design**  
  DAG enforcement as a rule, not a convention

- **Deterministic event-driven systems**  
  Same inputs always produce the same execution path and outputs

- **Clean separation of concerns (MVC)**  
  Domain model, controller, and view are strictly isolated

- **Practical use of classic design patterns**  
  Strategy, Decorator, Factory, Active Object, Publish–Subscribe

- **Safe concurrency**  
  Message queues and worker threads via Decorator + Active Object

- **Fail-fast validation**  
  Configuration errors are detected before execution begins

## Trade-offs & Limitations

- No feedback loops or iterative algorithms (by design)
- Single-process execution (JVM-local)
- No persistence or event replay
- Synchronous delivery within a Topic

These trade-offs are intentional and favor **predictability and debuggability**
over raw expressiveness.

## Potential Extensions

- Asynchronous Topics (non-blocking fan-out)
- Distributed Topics (Kafka / message brokers)
- Stateful Agents with explicit lifecycle and serialization
- Retry policies and dead-letter Topics
- Runtime metrics and critical-path visualization
- Static configuration analysis and linting

## Intended Audience

This project is intended as:

- A learning tool for **event-driven architecture**
- A portfolio demonstration of **architectural thinking**
- A foundation for more advanced **computation or workflow engines**

## Key Takeaway

> **Constraining a system correctly often matters more than adding features.**

This project shows how enforcing the right constraints can dramatically
simplify reasoning about complex event-driven behavior.
