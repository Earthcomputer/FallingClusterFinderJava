package net.earthcomputer.fallingclusterfinder;

import java.util.LinkedHashMap;
import java.util.Map;

public interface IStructureBuilder {
    default void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block, String... properties) {
        Map<String, String> map = new LinkedHashMap<>(properties.length / 2);
        for (int i = 0; i < properties.length; i += 2) {
            map.put(properties[i], properties[i + 1]);
        }
        fill(x1, y1, z1, x2, y2, z2, block, map);
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block, Map<String, String> properties);

    default void setblock(int x, int y, int z, String block, String... properties) {
        Map<String, String> map = new LinkedHashMap<>(properties.length / 2);
        for (int i = 0; i < properties.length; i += 2) {
            map.put(properties[i], properties[i + 1]);
        }
        setblock(x, y, z, block, map);
    }

    void setblock(int x, int y, int z, String block, Map<String, String> properties);
}
