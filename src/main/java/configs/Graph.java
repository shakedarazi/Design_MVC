package configs;

import graph.Agent;
import graph.Topic;
import graph.TopicManagerSingleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Graph extends ArrayList<Node> {
    public boolean hasCycles() {
        for (Node node : this) {
            if (node.hasCycles()) {
                return true;
            }
        }
        return false;
    }

    public void createFromTopics() {
        Map<String, Node> nodeMap = new HashMap<>();
        Collection<Topic> topics = TopicManagerSingleton.get().getTopics();
        for (Topic topic : topics) {
            String topicNodeName = "T" + topic.name;
            Node topicNode = nodeMap.computeIfAbsent(topicNodeName, name -> {
                Node n = new Node(name);
                n.setKind("TOPIC");
                return n;
            });
            for (Agent sub : topic.subs) {
                String agentNodeName = "A" + sub.getName();
                Node agentNode = nodeMap.computeIfAbsent(agentNodeName, name -> {
                    Node n = new Node(name);
                    n.setKind("AGENT");
                    return n;
                });
                topicNode.addEdge(agentNode);
            }
            for (Agent pub : topic.pubs) {
                String agentNodeName = "A" + pub.getName();
                Node agentNode = nodeMap.computeIfAbsent(agentNodeName, name -> {
                    Node n = new Node(name);
                    n.setKind("AGENT");
                    return n;
                });
                agentNode.addEdge(topicNode);
            }
        }
        this.clear();
        this.addAll(nodeMap.values());
    }
}
