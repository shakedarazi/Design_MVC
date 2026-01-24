package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ParallelAgent implements Agent {
    private static final class Task {
        private final String topic;
        private final Message msg;

        private Task(String topic, Message msg) {
            this.topic = topic;
            this.msg = msg;
        }
    }

    private final Agent agent;
    private final BlockingQueue<Task> queue;
    private final Thread worker;
    private volatile boolean running;

    public ParallelAgent(Agent agent, int capacity) {
        if (agent == null) {
            throw new NullPointerException("agent");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.agent = agent;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.running = true;

        this.worker = new Thread(this::runWorker, "ParallelAgent-" + agent.getName());
        this.worker.setDaemon(true);
        this.worker.start();
    }

    private void runWorker() {
        while (running) {
            try {
                Task task = queue.take();
                agent.callback(task.topic, task.msg);
            } catch (InterruptedException ex) {
                if (!running) {
                    break;
                }
                // otherwise, ignore and continue
            }
        }
    }

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void reset() {
        agent.reset();
    }

    @Override
    public void callback(String topic, Message msg) {
        if (!running) {
            return;
        }
        try {
            queue.put(new Task(topic, msg));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        running = false;
        worker.interrupt();
        try {
            worker.join(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        agent.close();
    }
}
