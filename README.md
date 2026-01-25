# Design_MVC

sequenceDiagram
    participant UI as View
    participant API as Controller
    participant T as Topics/Agents
    
    UI->>API: publish A=5
    API->>T: Topic A publish
    T-->>API: event TA
    T->>T: PlusAgent callback
    T-->>API: event TC
    T->>T: IncAgent callback
    T-->>API: event TD
    API-->>UI: SSE stream all events