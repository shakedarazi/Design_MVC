# Event-Driven DAG Computation Engine

A configuration-driven, event-based computation system that enforces directed acyclic graph (DAG) constraints and demonstrates strict architectural separation through design patterns.

## Project Overview

This system models computation as a network of stateless processors (Agents) connected by communication channels (Topics). The architecture prevents cyclic dependencies by enforcing a DAG constraint, ensuring that computation flows in a single direction and terminates deterministically.

Unlike trivial pipelines, this system supports:
- **Fan-out**: A single Topic can notify multiple downstream Agents
- **Fan-in**: An Agent can wait for multiple input Topics before computing
- **Dynamic reconfiguration**: The computation graph is defined declaratively at runtime
- **Strict validation**: Configurations containing cycles are rejected before execution begins

The system is not a general-purpose workflow engine. It demonstrates how architectural constraints (DAG enforcement, statelessness, configuration-driven design) create predictable, debuggable systems suitable for data processing, reactive computations, and event-driven architectures.

## Core Concepts

### Topics

Topics are named communication channels. They:
- Store no state (messages are ephemeral)
- Maintain subscriber and publisher lists
- Fan out messages to all subscribers synchronously
- Have package-private constructors to enforce creation through `TopicManager`

### Agents

Agents are stateless processors that:
- Subscribe to one or more input Topics
- Publish to one or more output Topics
- React to incoming messages via the `callback(String topic, Message msg)` method
- Compute results only when all required inputs are available
- Have deterministic, unique identities derived from their subscriptions and publications

Example: `PlusAgent` subscribes to Topics `A` and `B`, computes their sum when both are available, and publishes to Topic `C`.

### Events

Messages are immutable (`Message` class) and propagate deterministically:
- A publish to a Topic triggers immediate callbacks to all subscribers
- Callbacks execute sequentially in subscriber list order
- No guaranteed ordering across independent Topics

### DAG Constraints

The system enforces that the computation graph is a DAG:
- Topic → Agent (subscription) and Agent → Topic (publication) edges form a bipartite directed graph
- Before loading a configuration, the system performs depth-first search to detect cycles
- Cyclic configurations are rejected with an error

This constraint ensures:
- No infinite feedback loops
- Deterministic termination
- Predictable data flow

### Determinism

Determinism is guaranteed by:
- Stateless Agents (no hidden state between invocations)
- Synchronous message delivery within a Topic
- Explicit input availability checks (e.g., fan-in Agents wait for all inputs)
- Unique, stable Agent identities derived from configuration

## Configuration-Driven Design

Agent graphs are defined declaratively in a simple text format:

```
configs.PlusAgent
A,B
C

configs.IncAgent
C
D
```

Each Agent is declared with:
1. Fully-qualified class name
2. Comma-separated list of input Topics (subscriptions)
3. Comma-separated list of output Topics (publications)

This format decouples:
- **Behavior** (Agent implementation) from **wiring** (Topic connections)
- **Logic** (what the Agent computes) from **execution** (when and how it runs)

The `GenericConfig` loader uses reflection to instantiate Agents at runtime, enabling dynamic system composition without recompilation.

## Execution Model

### Publishing Semantics

When `TopicManager.getTopic("A").publish(new Message(5.0))` is called:
1. The Topic invokes `callback("A", msg)` on each subscribed Agent
2. Each Agent processes the message according to its internal logic
3. If an Agent has all required inputs, it computes a result and publishes to its output Topics
4. This triggers a cascade of further callbacks

### Fan-Out Behavior

A single Topic can have multiple subscribers. When a message is published:
- All subscribers are notified sequentially
- Each subscriber processes the message independently
- The order of notification follows subscriber registration order

### Fan-In Behavior

An Agent can subscribe to multiple Topics. For example, `PlusAgent` subscribes to `A` and `B`:
- It stores incoming values in internal fields (`x`, `y`)
- It maintains availability flags (`hasX`, `hasY`)
- It computes the sum only when both inputs are present
- It publishes the result to the output Topic

This pattern implements a synchronous join operation within the event-driven model.

### Event Ordering Guarantees

- **Within a Topic**: Subscribers are notified in registration order
- **Across Topics**: No ordering guarantees (independent Topics are logically concurrent)
- **Cascades**: When Agent A publishes to Topic T, which triggers Agent B, the entire cascade completes before control returns to the original publisher

## Architecture: MVC

The system enforces strict Model-View-Controller separation:

### Model (package `graph`)

Responsibilities:
- Define the computation graph primitives (`Topic`, `Agent`, `Message`)
- Manage Topic lifecycle and message propagation (`TopicManager`)
- Provide domain events via the `TopicEventListener` interface

The Model:
- **Does not** know about HTTP, JSON, or the View
- **Does not** initiate communication with the Controller
- **Exposes** domain events as callbacks, allowing the Controller to observe state changes

### Controller (package `app`)

Responsibilities:
- Expose REST API endpoints for configuration management and message publishing
- Translate HTTP requests into Model operations
- Register listeners on the Model to observe domain events
- Forward domain events to the View via Server-Sent Events (SSE)

The Controller:
- **Does not** manipulate View state directly
- **Does not** contain business logic (delegates to Model)
- **Acts as** a thin translation layer between HTTP and domain operations

### View (static resources: `index.html`, `app.js`, `graph-style.js`)

Responsibilities:
- Render the computation graph as a visual diagram (Cytoscape.js)
- Display real-time event stream (SSE consumer)
- Provide UI controls for user actions (load config, publish message, clear topic)

The View:
- **Must not** compute domain logic (e.g., cycle detection, message transformation)
- **Must not** directly access Model state (e.g., querying Topics)
- **May only** request actions via Controller endpoints and react to received events

This separation ensures:
- The Model can be tested independently of HTTP
- The View can be replaced (e.g., CLI, desktop GUI) without changing the Model
- The Controller remains a thin, testable adapter

## Design Patterns in Use

### Publish-Subscribe (Observer)

**Location**: `Topic` and `Agent` interaction

**Purpose**: Decouple message producers from consumers. Topics notify subscribers without knowing their concrete types or implementations.

**Implementation**: `Topic` maintains a list of `Agent` subscribers. When `publish(Message)` is called, it iterates and invokes `callback()` on each subscriber.

**Why appropriate**: Enables dynamic agent graphs where Topics can have zero, one, or many subscribers, and Agents can subscribe to multiple Topics.

---

### Strategy

**Location**: `Agent` interface with multiple implementations (`PlusAgent`, `MulAgent`, `IncAgent`, `DecAgent`)

**Purpose**: Define a family of algorithms (computation strategies) and make them interchangeable.

**Implementation**: All Agents implement the `Agent` interface, providing `callback()` logic specific to their operation. The system treats all Agents uniformly, regardless of their internal computation.

**Why appropriate**: New Agent types can be added without modifying existing code. The `GenericConfig` loader instantiates Agents via reflection, relying only on the `Agent` interface contract.

---

### Decorator

**Location**: `ParallelAgent` wrapping `Agent`

**Purpose**: Add asynchronous execution behavior to Agents without modifying their implementations.

**Implementation**: `ParallelAgent` implements the `Agent` interface, wraps an existing `Agent`, and delegates `callback()` invocations to a background thread via a `BlockingQueue`.

**Why appropriate**: Enables Agents to process messages asynchronously without blocking the publisher. The wrapped Agent remains unaware of the concurrency mechanism, preserving separation of concerns.

**Pattern note**: This is a structural decorator combined with the Active Object concurrency pattern (see below).

---

### Singleton

**Location**: `TopicManagerSingleton`

**Purpose**: Ensure a single, globally accessible instance of `TopicManager`.

**Implementation**: Inner static class pattern for lazy, thread-safe initialization:

```java
public class TopicManagerSingleton {
    private static class Holder {
        private static final TopicManager INSTANCE = new TopicManager();
    }
    public static TopicManager get() {
        return Holder.INSTANCE;
    }
}
```

**Why appropriate**: The `TopicManager` is a central registry. Multiple instances would fragment the Topic namespace, violating the constraint that Topics are uniquely identified by name.

---

### Factory (Reflection-Based)

**Location**: `GenericConfig.create()`

**Purpose**: Instantiate Agents dynamically based on configuration strings.

**Implementation**: Reads class names from a text file, uses `Class.forName()` and reflection to invoke constructors, and creates Agent instances at runtime.

**Why appropriate**: Enables configuration-driven system composition. New Agent types can be added to the system without modifying the loader, as long as they conform to the `Agent(String[], String[])` constructor signature.

**Trade-off**: Uses runtime reflection, sacrificing compile-time type safety for flexibility. Errors manifest as runtime exceptions if class names are incorrect or constructors are missing.

---

### Active Object

**Location**: `ParallelAgent` with `BlockingQueue` and dedicated worker thread

**Purpose**: Decouple method invocation from execution by queueing requests and processing them asynchronously.

**Implementation**:
- `callback()` enqueues a `Task` (topic + message) to a `BlockingQueue`
- A dedicated worker thread runs a loop: `queue.take()` → `agent.callback()` → repeat
- The calling thread returns immediately after enqueuing

**Why appropriate**: Prevents long-running Agent computations from blocking the publisher. The `BlockingQueue` provides thread-safe task submission and execution ordering.

**Concurrency note**: The worker thread processes tasks sequentially, preserving message order for each Agent.

---

### Flyweight (Partial)

**Location**: `TopicManager.getTopic(String name)` using `computeIfAbsent()`

**Purpose**: Share Topic instances across the system by name, avoiding duplicate objects.

**Implementation**: `ConcurrentHashMap<String, Topic>` ensures that `getTopic("A")` always returns the same instance.

**Why appropriate**: Topics are uniquely identified by name. Sharing instances prevents inconsistencies (e.g., multiple Topics with the same name but different subscriber lists).

**Pattern note**: This is a lightweight application of Flyweight. The primary benefit is identity consistency rather than memory optimization.

---

### Facade (Implicit)

**Location**: `ApiController` exposing simplified operations over the Model

**Purpose**: Provide a simplified HTTP interface over the complex graph construction and event propagation mechanisms.

**Implementation**: REST endpoints like `POST /api/config/load` hide the complexity of:
- File I/O for configuration parsing
- Reflection-based Agent instantiation
- Cycle detection via DFS
- Event listener registration

**Why appropriate**: Clients interact with high-level operations ("load this config") without understanding the internal graph construction process.

## Example: Configuration and Patterns in Action

Consider this configuration:

```
configs.PlusAgent
A,B
S

configs.IncAgent
S
S1
```

**Execution flow**:
1. User loads the config via `POST /api/config/load`
2. **Factory pattern**: `GenericConfig` uses reflection to instantiate `PlusAgent` and `IncAgent`
3. **Decorator pattern**: Each Agent is wrapped in a `ParallelAgent`
4. **Singleton pattern**: `TopicManagerSingleton.get()` provides the shared `TopicManager`
5. **Flyweight pattern**: `getTopic("A")` creates or retrieves the Topic `A`
6. **Strategy pattern**: Both Agents implement `Agent`, but have different `callback()` logic
7. User publishes `5.0` to `A` and `8.0` to `B` via `POST /api/topics/{name}/publish`
8. **Publish-Subscribe pattern**: Topic `A` notifies `PlusAgent`, which stores `5.0`
9. Topic `B` notifies `PlusAgent`, which now has both inputs, computes `13.0`, and publishes to `S`
10. **Active Object pattern**: `PlusAgent`'s worker thread processes the callback asynchronously
11. Topic `S` notifies `IncAgent`, which computes `14.0` and publishes to `S1`

**Patterns visible in the data flow**:
- **Strategy**: Different Agents (addition vs. increment) use the same callback mechanism
- **Decorator**: Asynchronous execution happens transparently
- **Publish-Subscribe**: Topics propagate messages without knowing Agent types

## Design Decisions

### Why Enforce a DAG?

Cycles enable feedback loops, which can cause:
- Infinite message cascades
- Non-terminating computations
- Unpredictable system behavior

By enforcing a DAG, the system guarantees:
- Every publish operation terminates
- The computation graph has a well-defined topology
- Debugging is simplified (data flows in one direction)

**Trade-off**: Systems requiring feedback (e.g., iterative algorithms, control loops) cannot be directly expressed. Such systems would require explicit iteration management outside the Agent graph.

### Why Are Agents Stateless?

Stateless Agents:
- Are easier to test (no hidden state to mock or reset)
- Enable deterministic replay (same inputs → same outputs)
- Simplify concurrency (no need for synchronization within an Agent)

**Partial exception**: Fan-in Agents like `PlusAgent` store temporary input values (`x`, `y`) between `callback()` invocations. However:
- This state is deterministic (derived entirely from input messages)
- It is cleared on `reset()`
- It is localized to the Agent (not shared across instances)

This design choice prioritizes simplicity and testability over expressiveness.

### Why Is Configuration Explicit?

Declarative configuration:
- Separates concerns (logic vs. wiring)
- Enables non-programmers to compose systems
- Facilitates runtime reconfiguration without recompilation
- Makes the computation graph visible and auditable

**Trade-off**: Configuration errors (typos in class names, incorrect Topic wiring) are caught at runtime rather than compile time. Static type systems (e.g., builder APIs) could improve safety at the cost of flexibility.

### Why Does Determinism Matter?

Deterministic systems:
- Produce repeatable results for testing and debugging
- Enable reproducible issue investigation (logs can be replayed)
- Simplify reasoning about correctness

This system achieves determinism by:
- Making Agents stateless
- Guaranteeing synchronous, ordered message delivery within a Topic
- Deriving Agent identities from configuration (not memory addresses or timestamps)

**Limitation**: Determinism applies to message propagation logic, not wall-clock timing. If Agents perform I/O or interact with external systems, non-determinism can be reintroduced.

## Educational and Portfolio Value

This project demonstrates:

**Software Engineering Principles**:
- **Separation of Concerns**: MVC architecture with clear boundaries
- **Single Responsibility**: Agents handle computation, Topics handle distribution, Controller handles HTTP
- **Dependency Inversion**: Code depends on abstractions (`Agent` interface) rather than concrete classes
- **Open/Closed**: New Agent types can be added without modifying existing code

**Design Patterns**:
- Practical application of classic patterns (Strategy, Decorator, Singleton, Factory, Publish-Subscribe)
- Pattern composition (Decorator + Active Object for concurrent Agents)
- Trade-offs in pattern selection (reflection-based Factory for flexibility vs. type safety)

**Architectural Constraints**:
- DAG enforcement as a design constraint
- Statelessness as a simplifying assumption
- Configuration-driven design for flexibility

**Concurrency**:
- Active Object pattern for asynchronous execution
- Thread-safe shared state (`ConcurrentHashMap`, `BlockingQueue`)
- Clean shutdown semantics (`ParallelAgent.close()`)

**Testing and Validation**:
- Cycle detection as a pre-execution validation step
- Deterministic behavior enabling repeatable tests
- Separation of concerns enabling unit testing of Model, Controller, and View independently

This project is suitable as:
- A learning tool for understanding event-driven architectures
- A portfolio demonstration of architectural thinking and pattern application
- A foundation for more complex systems (distributed event processing, workflow engines)

## Limitations and Future Work

### Current Limitations

1. **No Iteration**: DAG constraint prevents feedback loops. Iterative algorithms require external control (e.g., Controller-driven repeated publishes).

2. **Synchronous Execution Within Topics**: Message delivery blocks until all subscribers complete their `callback()` methods. Long-running Agents can delay downstream propagation, even with `ParallelAgent`.


3. **No Distributed Execution**: The system runs in a single JVM. Topics and Agents cannot span processes or machines.

4. **Limited Error Handling**: Exceptions in Agent `callback()` methods propagate to the publisher. There is no retry mechanism or dead-letter queue.

5. **No Persistence**: Message history is not stored. The system has no replay capability beyond re-publishing messages manually.

### Reasonable Extensions

**Asynchronous Topics**: Introduce a `TopicMode` (synchronous vs. asynchronous) to allow non-blocking message delivery. Asynchronous Topics would spawn threads or use an executor service to notify subscribers concurrently.

**Conditional Edges**: Allow Agents to selectively publish based on runtime conditions (e.g., "publish to `T` only if result > 0"). This would require a richer configuration format.

**Distributed Topics**: Implement Topics as message queues (e.g., Kafka topics) to enable distributed Agent execution. This would require addressing consistency, ordering, and failure handling.

**Error Recovery**: Add a `retry(int maxAttempts)` annotation to Agents and implement automatic retry logic in `ParallelAgent`. Failed messages could be routed to a dead-letter Topic for manual inspection.

**Stateful Agents**: Introduce a `StatefulAgent` interface with explicit state management (serialization, versioning, migration). This would enable windowing, aggregation, and iterative computations while preserving debuggability.

**Speculative Execution**: Allow Agents to produce partial results before all inputs are available (e.g., "assume missing input is 0"). This would require explicit handling of result invalidation if late inputs contradict assumptions.

**Graph Visualization Enhancements**: Add runtime metrics (message rates, Agent execution time, queue depths) to the View. Highlight critical paths and bottlenecks.

**Static Analysis**: Implement a compiler or linter that validates configurations at authoring time, checking for missing classes, incorrect constructor signatures, and potential type mismatches.