package tests;

import configs.Graph;
import configs.MathExampleConfig;
import configs.Node;
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

public class Exercise3SmokeTest {
    public static void main(String[] args) {
        TopicManagerSingleton.get().clear();

        MathExampleConfig config = new MathExampleConfig();
        config.create();

        final double[] result = new double[1];
        result[0] = Double.NaN;

        Agent resultAgent = new Agent() {
            @Override
            public String getName() {
                return "ResultAgent";
            }

            @Override
            public String getAgentId() {
                return "ResultAgent";
            }

            @Override
            public void reset() {
            }

            @Override
            public void callback(String topic, Message msg) {
                result[0] = msg.asDouble;
            }

            @Override
            public void onClearInput(String topic) {
            }

            @Override
            public void close() {
            }
        };

        TopicManagerSingleton.get().getTopic("R3").subscribe(resultAgent);

        TopicManagerSingleton.get().getTopic("A").publish(new Message(5.0));
        TopicManagerSingleton.get().getTopic("B").publish(new Message(8.0));

        double expected = (8.0 - 5.0) * (8.0 + 5.0);
        assert result[0] == expected : "Expected " + expected + " but got " + result[0];

        Graph g = new Graph();
        g.createFromTopics();

        String[] expectedNodes = {"TA", "TB", "TR1", "TR2", "TR3", "Aplus", "Aminus", "Amul"};
        for (String nodeName : expectedNodes) {
            boolean found = false;
            for (Node n : g) {
                if (n.getName().equals(nodeName)) {
                    found = true;
                    break;
                }
            }
            assert found : "Node " + nodeName + " not found in graph";
        }

        assert !g.hasCycles() : "Graph should not have cycles";

        System.out.println("OK");
    }
}
