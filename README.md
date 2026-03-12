# CascadeGraph
### ⚙️ Constraint-Driven Event Computation Engine

CascadeGraph is a configuration-driven, event-driven computation engine that models execution as an explicit **Directed Acyclic Graph (DAG)** of **Topics** and **Agents**.

Instead of letting event-driven behavior emerge implicitly from callbacks, mutable state, and timing quirks, CascadeGraph makes the computation graph a **first-class architectural artifact**: validated up front, structurally bounded, and visible during execution.

The project explores a simple but powerful idea:

> **Strong structural constraints can make event-driven systems dramatically easier to reason about, debug, and trust.**

---

## 🚀 Why this project exists

Event-driven systems often become difficult to reason about because the most important behavior is not explicit in the architecture.

Common problems include:

- ❌ hidden mutable state inside callbacks
- 🔁 cyclic dependencies and uncontrolled feedback loops
- 🔗 tight coupling between computation, orchestration, and presentation
- ⏱ behavior that depends more on timing than on structure

CascadeGraph explores a different design philosophy:

- 🧠 make the computation graph explicit
- 🔒 validate structural correctness before execution begins
- 🧩 isolate computation units from each other
- 📡 model dataflow directly instead of burying it in control flow
- 🛑 reject invalid topologies early instead of debugging them at runtime

This pushes complexity to **configuration validation time**, not to the middle of execution.

---

## ✨ What makes this project interesting

This is not just a calculator over a graph.

CascadeGraph is interesting because it treats **architecture itself as a correctness mechanism**.

It demonstrates:

- 🌳 **DAG-enforced execution** instead of permissive graph wiring
- 🔗 **Explicit dataflow** through Topics and Agents
- 🛡 **Isolated execution units** using Active Object–style wrappers
- 🧠 **Fan-in / fan-out semantics** as structural graph behavior
- 📡 **Live operational visibility** through graph visualization and event streaming
- 🧱 **Strict separation of concerns** between model, controller, and UI

The result is a system where key guarantees come from **structure**, not discipline.

---

## 🏗 Core architectural decisions

### 🔒 DAG enforced at load time

- graph topology is derived from configuration
- acyclicity is validated using **DFS-based cycle detection**
- cyclic graphs are rejected before execution begins

This guarantees:

- ✅ termination
- 🛑 no feedback loops
- 📐 structural correctness by construction

### 🧩 Explicit computation units (Agents)

Agents are narrow, reactive computation units.

- 📥 inputs are explicit through subscribed Topics
- 📤 outputs are explicit through published Topics
- 🔗 no implicit direct coupling between Agents

Some agents may maintain **local, bounded state** where needed, such as fan-in behavior. That state is explicit and intentional, never global or hidden.

### 🔀 Explicit fan-in / fan-out modeling

- 📡 fan-out: Topics broadcast events to multiple downstream Agents
- 🧮 fan-in: Agents wait for required inputs before publishing
- 🏛 dependencies are structural, not temporal

### 📝 Configuration-driven composition

- graph structure is defined declaratively at runtime
- wiring is decoupled from Agent implementation
- Agents are instantiated dynamically via reflection

### 🧱 Strict MVC separation

- **Model** — Topics, Agents, Graph, execution semantics
- **Controller** — REST API for config loading and event publishing
- **View** — Cytoscape.js visualization driven by SSE

The domain model is completely unaware of HTTP, JSON, browser state, or UI concerns.

---

## 🧠 Core concepts

### 📡 Topics

Topics are named, stateless pub/sub channels.

- 📢 they fan out events to subscribed Agents
- 🧼 they do not store message history
- 🗃 they are managed centrally through `TopicManager`

### ⚙️ Agents

Agents are reactive computation units.

- 📥 they subscribe to input Topics
- 📤 they publish to output Topics
- 🔄 they are driven by incoming events rather than direct invocation
- 🆔 they are uniquely identified per configuration

### 📦 Events

Events are immutable payloads that move through the graph.

- 🔐 immutable by design
- ⚡ delivered synchronously at the Topic level
- 🔒 structurally bounded by the DAG constraint

---

## 🔄 Execution model

At a high level, execution works as follows:

1. 📂 load textual configuration
2. 🏗 instantiate Agents via reflection
3. 🔌 wire Agents and Topics into a bipartite graph
4. 🔍 validate acyclicity and reject invalid graphs
5. 🧵 wrap Agents with execution decorators
6. 🌐 publish input events via REST
7. ⚡ propagate events across the graph
8. 🛑 terminate naturally because cyclic propagation is impossible

This makes execution behavior easier to reason about because propagation is **explicitly shaped by graph structure**.

---

## 🧵 Concurrency model

Each Agent is wrapped using an **Active Object–style decorator**.

That wrapper provides:

- 🧵 a dedicated worker thread per Agent
- 📬 a bounded queue per Agent
- 🔐 serialized execution per Agent

This gives the system several useful properties:

- 🛡 isolation between Agents
- 📏 predictable per-Agent execution semantics
- 🚦 bounded backpressure at the Agent boundary

### Important clarification

CascadeGraph does **not** guarantee one globally deterministic execution order across all Agents.

Its determinism is:

- ✅ **structural** — the graph is acyclic and execution terminates
- ✅ **local** — each Agent processes events serially
- ❌ **not globally schedule-deterministic** across the entire system

That trade-off is deliberate and explicit.

---

## 📺 Live visibility and UI

One of the strongest aspects of the project is that the computation model is not only explicit in code — it is also visible during runtime.

The UI allows you to:

- 🌐 load a demo computation graph
- ✍️ publish values into input Topics
- ✨ watch graph nodes highlight as propagation happens
- 📜 inspect the live event log stream
- 🧹 clear Topic values and observe downstream effects

This makes CascadeGraph not just a graph engine, but a **debuggable execution surface** for event-driven computation.

---

## 🧪 Example computation

Consider a simple configuration:

```text
configs.PlusAgent
A,B
S

configs.IncAgent
S
S1
```

This means:

- `PlusAgent` consumes `A` and `B`, then publishes to `S`
- `IncAgent` consumes `S`, then publishes to `S1`

### Example flow

If the user publishes:

- `A = 5`
- `B = 3`

Then the graph may evolve as:

1. `PlusAgent` receives both required inputs
2. it computes `S = 8`
3. `IncAgent` receives `S = 8`
4. it computes `S1 = 9`

In the UI, you can observe that propagation path directly through graph highlighting and live event logs.

---

## 🧠 Engineering highlights

- ✅ **Fail-fast DAG validation** before execution
- 🌳 **DFS-based cycle detection** as a structural correctness gate
- 🔗 **Explicit fan-in / fan-out semantics**
- 🧵 **Active Object–style Agent isolation** with bounded queues
- 🧱 **Strict MVC separation** between domain model, API, and visualization
- 📡 **SSE-based live updates** for runtime visibility
- 🏭 **Reflection-driven Agent instantiation** for configurable composition

---

## 🧩 Architectural mechanisms in practice

Rather than using design patterns as a checklist, CascadeGraph uses them to enforce architectural boundaries:

- 📢 **Publish–Subscribe** — Topics notify Agents without direct coupling
- 🎯 **Strategy-like computation units** — each Agent encapsulates one computation rule
- 🧵 **Decorator + Active Object** — execution behavior is separated from Agent logic
- 🏭 **Factory via reflection** — Agents are composed dynamically from configuration
- 🗂 **Centralized Topic registry** — preserves Topic identity and namespace consistency
- 🧱 **Facade-like controller layer** — REST entrypoints hide orchestration details from the UI

The important point is not the pattern names themselves, but the constraints they help preserve.

---

## ⚖️ Trade-offs and limitations

CascadeGraph is intentionally constrained.

### By design, it does not support:

- 🚫 feedback loops
- 🔀 global execution ordering guarantees
- 🌍 distributed multi-process execution
- 💾 persistence or event replay
- 📬 stronger-than-at-most-once delivery semantics

These constraints prioritize:

- 🧠 structural correctness
- 🔎 debuggability
- 📐 predictability
- 🧩 explicit execution semantics

over maximal expressiveness or distributed throughput.

---

## 🚀 Potential extensions

- 🧮 deterministic schedulers or topological execution modes
- 🌍 distributed Topics via Kafka or Redis Streams
- 🧠 richer stateful Agents with lifecycle management
- 🔁 retry policies and dead-letter Topics
- 📊 metrics, tracing, and critical-path analysis
- 🧾 static configuration linting and richer validation

---

## ⚙️ How to run

### Prerequisites

- ☕ Java 17+
- 📦 Maven 3.6+

### Steps

#### 1. Clone the repository

```bash
git clone <repository-url>
cd Design_MVC
```

#### 2. Build and start the application

```bash
mvn spring-boot:run
```

#### 3. Open the UI

```text
http://localhost:8080/
```

#### 4. Try the demo

- Click **Load Config** to load the demo computation graph
- Enter a Topic name such as `A` and a value such as `5`, then click **Publish**
- Watch the graph highlight as events propagate through the DAG
- Use the **Event Log** panel to inspect the live stream of system events
- Click **Clear** on a Topic to reset its value and observe downstream effects

---

## 👥 Intended audience

- 🎓 engineers learning event-driven or dataflow architectures
- 💼 portfolio reviewers evaluating system-level thinking
- 🧱 developers interested in workflow engines and constrained execution models

---

## ✅ What this project demonstrates

CascadeGraph demonstrates how to design event-driven systems around **explicit structure and enforced constraints**.

More specifically, it shows:

- 🌳 graph-validated computation
- 🔗 explicit dataflow modeling
- 🛑 fail-fast structural validation
- 🧵 isolated reactive execution units
- 🧱 clean architectural separation
- 📡 live runtime visibility through visualization and SSE

---

## 🧠 Key takeaway

Constraining a system correctly often matters more than making it more powerful.

In CascadeGraph, **explicit structure, isolation, and validation** simplify reasoning about behavior that would otherwise become difficult to debug in traditional event-driven designs.

---

## ✨ Credits

Designed and implemented by **Shaked Arazi**.

Architectural design, concurrency model, validation engine, and execution semantics were built with a strong emphasis on structural correctness and explicit dataflow thinking.

