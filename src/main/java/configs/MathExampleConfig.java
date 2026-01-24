package configs;

import graph.BinOpAgent;

public class MathExampleConfig implements Config {
    @Override
    public void create() {
        new BinOpAgent("plus", "A", "B", "R1", (x, y) -> x + y);
        new BinOpAgent("minus", "A", "B", "R2", (x, y) -> y - x);
        new BinOpAgent("mul", "R1", "R2", "R3", (x, y) -> x * y);
    }

    @Override
    public String getName() {
        return "Math Example";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void close() {
    }
}
