package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

public class IncAgent implements Agent {
    private final String[] subs;
    private final String[] pubs;

    public IncAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
        TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
        TopicManagerSingleton.get().getTopic(pubs[0]).addPublisher(this);
    }

    @Override
    public String getName() {
        return "IncAgent";
    }

    @Override
    public String getAgentId() {
        return "IncAgent[" + String.join(",", subs) + "->" + String.join(",", pubs) + "]";
    }

    @Override
    public void reset() {
    }

    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) {
            return;
        }
        TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(msg.asDouble + 1), getAgentId());
    }

    @Override
    public void close() {
    }

    @Override
    public void onClearInput(String topic) {
        // Stateless agent - nothing to clear
    }
}
