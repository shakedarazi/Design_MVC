package graph;

import java.util.ArrayList;
import java.util.List;

public class Topic {
    public final String name;
    public final List<Agent> subs;
    public final List<Agent> pubs;

    private static TopicEventListener listener;

    public static void setListener(TopicEventListener l) {
        listener = l;
    }

    Topic(String name) {
        this.name = name;
        this.subs = new ArrayList<>();
        this.pubs = new ArrayList<>();
    }

    public void subscribe(Agent agent) {
        if (!subs.contains(agent)) {
            subs.add(agent);
        }
    }

    public void unsubscribe(Agent agent) {
        subs.remove(agent);
    }

    public void publish(Message msg) {
        if (listener != null) {
            listener.onPublish(name, msg);
        }
        for (Agent agent : subs) {
            agent.callback(name, msg);
        }
    }

    public void addPublisher(Agent agent) {
        if (!pubs.contains(agent)) {
            pubs.add(agent);
        }
    }

    public void removePublisher(Agent agent) {
        pubs.remove(agent);
    }
}
