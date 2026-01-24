package app;

import configs.GenericConfig;
import configs.Graph;
import configs.Node;
import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api")
public class ApiController {

    private GenericConfig activeConfig;

    public record ConfigLoadRequest(String configText) {}
    public record PublishRequest(String type, String value) {}
    public record FlowEvent(long ts, EventType type, String from, Double value) {}
    public enum EventType { INPUT_PUBLISH }

    private static final class EventBus {
        private static final int MAX_EVENTS = 500;
        private static final LinkedList<FlowEvent> events = new LinkedList<>();
        private static final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        static synchronized void emit(FlowEvent event) {
            events.addLast(event);
            while (events.size() > MAX_EVENTS) {
                events.removeFirst();
            }
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().data(event));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            }
        }

        static synchronized List<FlowEvent> getEvents(int limit) {
            int count = Math.min(limit, events.size());
            List<FlowEvent> result = new ArrayList<>(count);
            Iterator<FlowEvent> it = events.descendingIterator();
            while (it.hasNext() && result.size() < count) {
                result.add(it.next());
            }
            Collections.reverse(result);
            return result;
        }

        static void addEmitter(SseEmitter emitter) {
            emitters.add(emitter);
            emitter.onCompletion(() -> emitters.remove(emitter));
            emitter.onTimeout(() -> emitters.remove(emitter));
            emitter.onError(e -> emitters.remove(emitter));
        }
    }

    @PostMapping("/config/load")
    public Map<String, Object> loadConfig(@RequestBody ConfigLoadRequest request) {
        try {
            if (activeConfig != null) {
                activeConfig.close();
                TopicManagerSingleton.get().clear();
                activeConfig = null;
            }
            Path tempFile = Files.createTempFile("config", ".txt");
            Files.writeString(tempFile, request.configText());
            GenericConfig gc = new GenericConfig();
            gc.setConfFile(tempFile.toString());
            gc.create();
            activeConfig = gc;
            List<String> topicNames = new ArrayList<>();
            for (Topic t : TopicManagerSingleton.get().getTopics()) {
                topicNames.add(t.name);
            }
            Collections.sort(topicNames);
            return Map.of("ok", true, "topics", topicNames);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/config/unload")
    public Map<String, Object> unloadConfig() {
        if (activeConfig != null) {
            activeConfig.close();
            TopicManagerSingleton.get().clear();
            activeConfig = null;
        }
        return Map.of("ok", true);
    }

    @PostMapping("/topics/{name}/publish")
    public Map<String, Object> publish(@PathVariable String name, @RequestBody PublishRequest request) {
        Message msg;
        Double eventValue = null;
        if ("double".equals(request.type())) {
            double d = Double.parseDouble(request.value());
            msg = new Message(d);
            eventValue = d;
        } else {
            msg = new Message(request.value());
        }
        TopicManagerSingleton.get().getTopic(name).publish(msg);
        EventBus.emit(new FlowEvent(System.currentTimeMillis(), EventType.INPUT_PUBLISH, "T" + name, eventValue));
        return Map.of("ok", true);
    }

    @GetMapping("/topics")
    public Map<String, Object> getTopics() {
        List<String> topicNames = new ArrayList<>();
        for (Topic t : TopicManagerSingleton.get().getTopics()) {
            topicNames.add(t.name);
        }
        Collections.sort(topicNames);
        return Map.of("topics", topicNames);
    }

    @GetMapping("/graph")
    public Map<String, Object> getGraph() {
        Graph g = new Graph();
        g.createFromTopics();
        List<Map<String, String>> nodes = new ArrayList<>();
        List<Map<String, String>> edges = new ArrayList<>();
        for (Node node : g) {
            String id = node.getName();
            String kind = id.startsWith("T") ? "TOPIC" : "AGENT";
            nodes.add(Map.of("id", id, "kind", kind));
            for (Node neighbor : node.getEdges()) {
                edges.add(Map.of("from", id, "to", neighbor.getName()));
            }
        }
        return Map.of("nodes", nodes, "edges", edges);
    }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(0L);
        EventBus.addEmitter(emitter);
        return emitter;
    }

    @GetMapping("/events")
    public Map<String, Object> getEvents(@RequestParam(defaultValue = "50") int limit) {
        return Map.of("events", EventBus.getEvents(limit));
    }
}
