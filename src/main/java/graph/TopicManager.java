package graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TopicManager {
    private final Map<String, Topic> topics;

    public TopicManager() {
        this.topics = new HashMap<>();
    }

    public Topic getTopic(String name) {
        Topic existing = topics.get(name);
        if (existing != null) {
            return existing;
        }
        Topic created = new Topic(name);
        topics.put(name, created);
        return created;
    }

    public Collection<Topic> getTopics() {
        return topics.values();
    }

    public void clear() {
        topics.clear();
    }
}
