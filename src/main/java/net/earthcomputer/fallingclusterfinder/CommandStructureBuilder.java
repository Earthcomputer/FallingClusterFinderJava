package net.earthcomputer.fallingclusterfinder;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandStructureBuilder implements IStructureBuilder {
    private final Consumer<String> commandConsumer;

    public CommandStructureBuilder(Consumer<String> commandConsumer) {
        this.commandConsumer = commandConsumer;
    }

    @Override
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block, Map<String, String> properties) {
        if (!properties.isEmpty()) {
            block += " " + properties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
        }
        commandConsumer.accept(String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block));
    }

    @Override
    public void setblock(int x, int y, int z, String block, Map<String, String> properties) {
        if (!properties.isEmpty()) {
            block += " " + properties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
        }
        commandConsumer.accept(String.format("setblock %d %d %d %s", x, y, z, block));
    }
}
