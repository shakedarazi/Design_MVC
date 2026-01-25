package graph;

public interface TopicEventListener {
    void onPublish(String topicName, Message msg);
}
