package graph;

public final class TopicManagerSingleton {
    private TopicManagerSingleton() {
    }

    private static class Holder {
        private static final TopicManager INSTANCE = new TopicManager();
    }

    public static TopicManager get() {
        return Holder.INSTANCE;
    }
}
