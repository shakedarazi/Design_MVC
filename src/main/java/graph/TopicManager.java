package graph;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TopicManager {
    private final ConcurrentHashMap<String, Topic> topics;

    public TopicManager() {
        this.topics = new ConcurrentHashMap<>();
    }

    public Topic getTopic(String name) {
        return topics.computeIfAbsent(name, Topic::new);
    }

    public Collection<Topic> getTopics() {
        return List.copyOf(topics.values());
    }

    public void clear() {
        topics.clear();
    }
}
