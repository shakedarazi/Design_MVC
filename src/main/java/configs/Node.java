package configs;

import graph.Message;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {
    private String name;
    private String kind;  // "TOPIC" or "AGENT"
    private List<Node> edges;
    private Message message;

    public Node(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<Node> getEdges() {
        return edges;
    }

    public void setEdges(List<Node> edges) {
        this.edges = edges;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void addEdge(Node n) {
        edges.add(n);
    }

    public boolean hasCycles() {
        Set<Node> visited = new HashSet<>();
        Set<Node> recStack = new HashSet<>();
        return hasCyclesDFS(this, visited, recStack);
    }

    private boolean hasCyclesDFS(Node node, Set<Node> visited, Set<Node> recStack) {
        if (recStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        visited.add(node);
        recStack.add(node);
        for (Node neighbor : node.edges) {
            if (hasCyclesDFS(neighbor, visited, recStack)) {
                return true;
            }
        }
        recStack.remove(node);
        return false;
    }
}
