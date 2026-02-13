# ⚙️ Event-Driven DAG Computation Engine

A **configuration-driven, event-driven computation engine** that models computation as an explicit **Directed Acyclic Graph (DAG)** of Topics and Agents.

The project explores how **strong architectural constraints** (DAG enforcement, explicit dataflow, isolated execution units, and separation of concerns) can significantly improve **reasoning, debuggability, and correctness** in event-driven systems.

---

## 🎯 Why This Project Exists

Event-driven systems often become difficult to reason about due to:

- ❌ Implicit mutable state hidden inside callbacks
- 🔁 Cyclic dependencies and uncontrolled feedback loops
- 🔗 Tight coupling between computation, orchestration, and presentation
- ⏱ Execution behavior depending on timing and scheduling rather than structure

This project explores an alternative design philosophy:

> **🧠 Make the computation graph an explicit, validated, first-class architectural artifact.**

Instead of relying on conventions or discipline, the system enforces structural constraints up-front and pushes complexity to **configuration validation time**, not runtime.

---

## 🏗 Core Architectural Decisions

### 🔒 DAG Enforced at Load Time

- 🌳 Graph topology derived from configuration
- 🔍 Validated using DFS-based cycle detection
- 🚫 Cyclic graphs rejected before execution begins

This guarantees:

- ✅ Termination
- 🛑 No feedback loops
- 📐 Structural correctness by construction

### 🧩 Explicit Computation Units (Agents)

- 🧠 Agents implement a narrow computation interface
- 📥 Inputs and 📤 outputs are explicit via Topics
- 🔗 No implicit coupling between Agents

> ℹ️ Some agents maintain explicit, local input state (e.g., fan-in agents).  
> State is visible, bounded, and intentional — never global or hidden.

### 🔀 Explicit Fan-In / Fan-Out Modeling

- 📡 **Fan-out**: Topics broadcast events to multiple downstream Agents
- 🧮 **Fan-in**: Agents wait for multiple required inputs before publishing
- 🏛 Dependencies are structural, not temporal

### 📝 Configuration-Driven Composition

- 🗂 Graph structure defined declaratively at runtime
- 🔌 Wiring fully decoupled from Agent implementation
- 🏭 Agents instantiated dynamically via reflection

### 🧱 Strict MVC Separation

- 🧩 **Model** — Topics, Agents, Graph, execution semantics
- 🎛 **Controller** — REST API for config loading & event publishing
- 🖥 **View** — Cytoscape.js visualization via SSE

The domain model is completely unaware of HTTP, JSON, or UI concerns.

---

## 🧠 Core Concepts

### 📡 Topics

- Named, stateless pub/sub channels
- 📢 Fan-out events synchronously to subscribed Agents
- 🧼 No message history persistence
- 🗃 Managed centrally via TopicManager

### ⚙️ Agents

- Reactive computation units
- 📥 Subscribe to input Topics
- 📤 Publish to output Topics
- 🔄 Driven purely by events (no direct invocation)
- 🆔 Uniquely identified per configuration

### 📦 Events

- 🔐 Immutable payloads
- ⚡ Delivered synchronously at Topic level
- 🔒 Cascading execution structurally bounded by DAG

---

## 🔄 Execution Model (High-Level)

1. 📂 Load textual configuration
2. 🏗 Instantiate Agents via reflection
3. 🔌 Wire Agents and Topics into a bipartite graph
4. 🔍 Validate acyclicity (fail-fast)
5. 🧵 Wrap Agents with execution decorator
6. 🌐 Publish input events via REST
7. ⚡ Event-driven cascade across graph
8. 🛑 Guaranteed termination due to DAG constraint

---

## 🧵 Concurrency Model

- Each Agent wrapped using **Active Object–style decorator**
- 🧵 Dedicated worker thread + bounded queue per Agent
- 🔐 Serialized execution per Agent

Provides:

- 🛡 Isolation between Agents
- 📏 Predictable per-Agent execution semantics
- 🚦 Backpressure via bounded queues

**Important clarification:**

> ⚠️ **No globally deterministic execution order across Agents.**  
> Determinism is structural (termination, DAG correctness) and **per-Agent**, not system-wide scheduling.  
> This trade-off is deliberate and explicit.

---

## 🧩 Design Patterns in Practice

This project **composes patterns to enforce architectural constraints**, not to showcase patterns superficially.

- 📢 **Publish–Subscribe** — Topics notify Agents without coupling
- 🎯 **Strategy** — Agents encapsulate computation logic
- 🧵 **Decorator + Active Object** — Separate execution from logic
- 🏭 **Factory (Reflection-Based)** — Dynamic Agent instantiation
- 🗂 **Singleton** — TopicManager enforces namespace consistency
- 🪶 **Flyweight (Partial)** — Shared Topics preserve identity
- 🧱 **Facade** — REST controller simplifies orchestration

---

## 📝 Example Configuration

```text
configs.PlusAgent
A,B
S

configs.IncAgent
S
S1
```

---

## 🚀 What This Project Demonstrates

- 🏛 Architectural constraint design
- 📊 Explicit dataflow modeling
- 🛑 Fail-fast validation
- 🔐 Isolated execution units
- 🧱 Clean MVC separation
- 📡 Live operational visibility (SSE + graph visualization)

---

## ⚖️ Trade-offs & Limitations

- 🚫 No feedback loops (by design)
- 🔀 No global execution ordering guarantees
- 🖥 Single-process JVM runtime
- 💾 No persistence or event replay
- 📬 At-most-once delivery semantics

These trade-offs prioritize:

- 🧠 Structural correctness
- 🔎 Debuggability
- 📐 Predictability

over maximal expressiveness or distributed throughput.

---

## 🔮 Potential Extensions

- 🧮 Deterministic schedulers / topological execution
- 🌍 Distributed Topics (Kafka / Redis Streams)
- 🧠 Stateful Agents with lifecycle management
- 🔁 Retry policies + dead-letter Topics
- 📊 Metrics, tracing, and critical-path analysis
- 🧾 Static configuration linting

---

## 👥 Intended Audience

- 🎓 Engineers learning event-driven & dataflow architectures
- 💼 Portfolio reviewers evaluating system-level thinking
- 🧱 Developers building workflow/orchestration engines

---

## 🧠 Key Takeaway

> **Constraining a system correctly often matters more than making it more powerful.**

Explicit structure and validation dramatically simplify reasoning about complex event-driven behavior.

---

## ✨ Credits

**🚀 Designed & Implemented by**  
**Shaked Arazi**

Architectural design, concurrency model, validation engine, and execution semantics crafted with a strong emphasis on structural correctness and explicit dataflow thinking.
