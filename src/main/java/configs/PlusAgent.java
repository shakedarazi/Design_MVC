package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

public class PlusAgent implements Agent {
    private final String[] subs;
    private final String[] pubs;
    private double x;
    private double y;
    private boolean hasX;
    private boolean hasY;

    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
        TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
        TopicManagerSingleton.get().getTopic(subs[1]).subscribe(this);
        TopicManagerSingleton.get().getTopic(pubs[0]).addPublisher(this);
    }

    @Override
    public String getName() {
        return "PlusAgent";
    }

    @Override
    public String getAgentId() {
        return "PlusAgent[" + String.join(",", subs) + "->" + String.join(",", pubs) + "]";
    }

    @Override
    public void reset() {
        x = 0;
        y = 0;
        hasX = false;
        hasY = false;
    }

    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) {
            return;
        }
        if (topic.equals(subs[0])) {
            x = msg.asDouble;
            hasX = true;
        } else if (topic.equals(subs[1])) {
            y = msg.asDouble;
            hasY = true;
        }
        if (hasX && hasY) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(x + y), getAgentId());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void onClearInput(String topic) {
        if (topic.equals(subs[0])) {
            hasX = false;
        } else if (topic.equals(subs[1])) {
            hasY = false;
        }
    }
}
