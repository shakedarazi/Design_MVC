package graph;

import java.util.function.BinaryOperator;

public class BinOpAgent implements Agent {
    private final String name;
    private final String in1Topic;
    private final String in2Topic;
    private final String outTopic;
    private final BinaryOperator<Double> op;
    private double x;
    private double y;
    private boolean hasX;
    private boolean hasY;

    public BinOpAgent(String name, String in1Topic, String in2Topic, String outTopic, BinaryOperator<Double> op) {
        this.name = name;
        this.in1Topic = in1Topic;
        this.in2Topic = in2Topic;
        this.outTopic = outTopic;
        this.op = op;
        TopicManagerSingleton.get().getTopic(in1Topic).subscribe(this);
        TopicManagerSingleton.get().getTopic(in2Topic).subscribe(this);
        TopicManagerSingleton.get().getTopic(outTopic).addPublisher(this);
    }

    @Override
    public String getName() {
        return name;
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
        if (topic.equals(in1Topic)) {
            x = msg.asDouble;
            hasX = true;
        } else if (topic.equals(in2Topic)) {
            y = msg.asDouble;
            hasY = true;
        }
        if (hasX && hasY) {
            double r = op.apply(x, y);
            TopicManagerSingleton.get().getTopic(outTopic).publish(new Message(r), getName());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void onClearInput(String topic) {
        if (topic.equals(in1Topic)) {
            hasX = false;
        } else if (topic.equals(in2Topic)) {
            hasY = false;
        }
    }
}
