package tests;

import configs.GenericConfig;
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Exercise4SmokeTest {
    public static void main(String[] args) throws Exception {
        TopicManagerSingleton.get().clear();

        Path tempFile = Files.createTempFile("config", ".txt");
        Files.write(tempFile, Arrays.asList(
            "configs.PlusAgent",
            "A,B",
            "C",
            "configs.IncAgent",
            "C",
            "D"
        ));

        GenericConfig gc = new GenericConfig();
        gc.setConfFile(tempFile.toString());
        gc.create();

        final double[] captured = new double[1];
        captured[0] = Double.NaN;

        Agent captureAgent = new Agent() {
            @Override
            public String getName() {
                return "CaptureAgent";
            }

            @Override
            public String getAgentId() {
                return "CaptureAgent";
            }

            @Override
            public void reset() {
            }

            @Override
            public void callback(String topic, Message msg) {
                captured[0] = msg.asDouble;
            }

            @Override
            public void onClearInput(String topic) {
            }

            @Override
            public void close() {
            }
        };

        TopicManagerSingleton.get().getTopic("D").subscribe(captureAgent);

        TopicManagerSingleton.get().getTopic("A").publish(new Message(5.0));
        TopicManagerSingleton.get().getTopic("B").publish(new Message(8.0));

        Thread.sleep(500);

        assert captured[0] == 14.0 : "Expected 14.0 but got " + captured[0];

        gc.close();
        gc.close();

        Files.deleteIfExists(tempFile);

        System.out.println("OK");
    }
}
