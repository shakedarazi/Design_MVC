package graph;

public interface TopicEventListener {
    void onPublish(String topicName, Message msg);
    void onClear(String topicName);
    void onAgentPublish(String agentName, String topicName, Message msg);
}
