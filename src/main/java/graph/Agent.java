package graph;

public interface Agent {
    String getName();
    String getAgentId();  // MANDATORY - unique, deterministic ID for graph/event identity
    void reset();
    void callback(String topic, Message msg);
    void onClearInput(String topic);
    void close();
}
