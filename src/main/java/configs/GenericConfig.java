package configs;

import graph.Agent;
import graph.ParallelAgent;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GenericConfig implements Config {
    private String confFile;
    private final List<ParallelAgent> runningAgents = new ArrayList<>();

    public void setConfFile(String confFile) {
        this.confFile = confFile;
    }

    @Override
    public String getName() {
        return "Generic Config";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void create() {
        try {
            List<String> allLines = Files.readAllLines(Paths.get(confFile));
            List<String> lines = new ArrayList<>();
            for (String line : allLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
            if (lines.size() % 3 != 0) {
                throw new IllegalArgumentException("Config file lines must be divisible by 3");
            }
            for (int i = 0; i < lines.size(); i += 3) {
                String className = lines.get(i);
                String subsLine = lines.get(i + 1);
                String pubsLine = lines.get(i + 2);

                String[] subsArr = parseTopics(subsLine);
                String[] pubsArr = parseTopics(pubsLine);

                Class<?> clazz = Class.forName(className);
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) ctor.newInstance(subsArr, pubsArr);

                ParallelAgent wrapper = new ParallelAgent(agent, 100);
                runningAgents.add(wrapper);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] parseTopics(String line) {
        if (line.isEmpty()) {
            return new String[0];
        }
        String[] parts = line.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim();
        }
        return result;
    }

    @Override
    public void close() {
        for (ParallelAgent pa : runningAgents) {
            try {
                pa.close();
            } catch (Exception ignored) {
            }
        }
        runningAgents.clear();
    }
}
