package net.earthcomputer.fallingclusterfinder;

import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

import javax.swing.JOptionPane;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LitematicStructureBuilder implements IStructureBuilder {
    private static class BlockState {
        private final String block;
        private final Map<String, String> properties;

        private BlockState(String block, Map<String, String> properties) {
            this.block = block;
            this.properties = properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlockState that = (BlockState) o;

            if (!block.equals(that.block)) return false;
            return properties.equals(that.properties);
        }

        @Override
        public int hashCode() {
            int result = block.hashCode();
            result = 31 * result + properties.hashCode();
            return result;
        }
    }

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private final Map<BlockState, Integer> palette = new HashMap<>();
    private LitematicaBitArray storage = new LitematicaBitArray(2, 1);

    @Override
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block, Map<String, String> properties) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setblock(x, y, z, block, properties);
                }
            }
        }
    }

    @Override
    public void setblock(int x, int y, int z, String block, Map<String, String> properties) {
        if (!block.contains(":")) {
            block = "minecraft:" + block;
        }
        if (palette.isEmpty()) {
            minX = x;
            minY = y;
            minZ = z;
            maxX = x;
            maxY = y;
            maxZ = z;
            palette.put(new BlockState("minecraft:air", new HashMap<>()), 0);
        } else {
            int minX = this.minX, minY = this.minY, minZ = this.minZ;
            int maxX = this.maxX, maxY = this.maxY, maxZ = this.maxZ;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
            if (minX != this.minX || minY != this.minY || minZ != this.minZ || maxX != this.maxX || maxY != this.maxY || maxZ != this.maxZ) {
                LitematicaBitArray newBitArray = new LitematicaBitArray(storage.bitsPerEntry, (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
                for (int newX = minX; newX <= maxX; newX++) {
                    for (int newY = minY; newY <= maxY; newY++) {
                        for (int newZ = minZ; newZ <= maxZ; newZ++) {
                            int oldValue;
                            if (newX >= this.minX && newX <= this.maxX && newY >= this.minY && newY <= this.maxY && newZ >= this.minZ && newZ <= this.maxZ) {
                                oldValue = storage.getAt((long) (newY - this.minY) * (this.maxX - this.minX + 1) * (this.maxZ - this.minZ + 1) + (long) (newZ - this.minZ) * (this.maxX - this.minX + 1) + (newX - this.minX));
                            } else {
                                oldValue = 0;
                            }
                            newBitArray.setAt((long) (newY - minY) * (maxX - minX + 1) * (maxZ - minZ + 1) + (long) (newZ - minZ) * (maxX - minX + 1) + (newX - minX), oldValue);
                        }
                    }
                }
                this.minX = minX;
                this.minY = minY;
                this.minZ = minZ;
                this.maxX = maxX;
                this.maxY = maxY;
                this.maxZ = maxZ;
                this.storage = newBitArray;
            }
        }

        int index = palette.computeIfAbsent(new BlockState(block, properties), k -> palette.size());
        if ((index & (index - 1)) == 0) {
            int bitsRequired = (int) Math.ceil(Math.log(palette.size()) / Math.log(2));
            if (bitsRequired > storage.bitsPerEntry) {
                LitematicaBitArray newBitArray = new LitematicaBitArray(bitsRequired, storage.size());
                for (int i = 0; i < storage.size(); i++) {
                    newBitArray.setAt(i, storage.getAt(i));
                }
                storage = newBitArray;
            }
        }

        storage.setAt((long) (y - minY) * (maxX - minX + 1) * (maxZ - minZ + 1) + (long) (z - minZ) * (maxX - minX + 1) + (x - minX), index);
    }

    public void save(File dest) {
        CompoundTag rootTag = new CompoundTag();
        rootTag.putInt("MinecraftDataVersion", 1343);
        rootTag.putInt("Version", 4);
        CompoundTag metadata = new CompoundTag();
        long time = System.currentTimeMillis();
        metadata.putLong("TimeCreated", time);
        metadata.putLong("TimeModified", time);
        CompoundTag enclosingSize = new CompoundTag();
        enclosingSize.putInt("x", maxX - minX + 1);
        enclosingSize.putInt("y", maxY - minY + 1);
        enclosingSize.putInt("z", maxZ - minZ + 1);
        metadata.put("EnclosingSize", enclosingSize);
        metadata.putString("Description", "Never see the lite of day"); // lol
        metadata.putInt("RegionCount", 1);
        long totalBlocks = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int index = storage.getAt((long) (y - minY) * (maxX - minX + 1) * (maxZ - minZ + 1) + (long) (z - minZ) * (maxX - minX + 1) + (x - minX));
                    if (index != 0) {
                        totalBlocks++;
                    }
                }
            }
        }
        metadata.putLong("TotalBlocks", totalBlocks);
        metadata.putString("Author", "RaysWorks"); // lol
        metadata.putLong("TotalVolume", (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
        metadata.putString("Name", "Cluster Chunks");
        rootTag.put("Metadata", metadata);
        CompoundTag regions = new CompoundTag();
        CompoundTag region = new CompoundTag();
        region.put("PendingBlockTicks", new ListTag<>(CompoundTag.class));
        CompoundTag position = new CompoundTag();
        position.putInt("x", 0);
        position.putInt("y", 0);
        position.putInt("z", 0);
        region.put("Position", position);
        region.put("Size", enclosingSize);
        region.put("TileEntities", new ListTag<>(CompoundTag.class));
        region.put("Entities", new ListTag<>(CompoundTag.class));
        ListTag<CompoundTag> blockStatePalette = new ListTag<>(CompoundTag.class);
        for (int i = 0; i < palette.size(); i++) {
            blockStatePalette.add(new CompoundTag());
        }
        for (Map.Entry<BlockState, Integer> entry : palette.entrySet()) {
            CompoundTag blockState = blockStatePalette.get(entry.getValue());
            blockState.putString("Name", entry.getKey().block);
            if (!entry.getKey().properties.isEmpty()) {
                CompoundTag properties = new CompoundTag();
                for (Map.Entry<String, String> property : entry.getKey().properties.entrySet()) {
                    properties.putString(property.getKey(), property.getValue());
                }
                blockState.put("Properties", properties);
            }
        }
        region.put("BlockStatePalette", blockStatePalette);
        region.putLongArray("BlockStates", storage.getBackingLongArray());
        regions.put("Cluster Chunks", region);
        rootTag.put("Regions", regions);

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            new NBTSerializer().toStream(new NamedTag("", rootTag), out);
            JOptionPane.showMessageDialog(null, "Saved litematic. The placement position should be " + minX + ", " + minY + ", " + minZ + ".");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to save file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Class copied from Litematica, license LGPL-3.0
    public static class LitematicaBitArray
    {
        /** The long array that is used to store the data for this BitArray. */
        private final long[] longArray;
        /** Number of bits a single entry takes up */
        private final int bitsPerEntry;
        /**
         * The maximum value for a single entry. This also works as a bitmask for a single entry.
         * For instance, if bitsPerEntry were 5, this value would be 31 (ie, {@code 0b00011111}).
         */
        private final long maxEntryValue;
        /** Number of entries in this array (<b>not</b> the length of the long array that internally backs this array) */
        private final long arraySize;

        public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn)
        {
            this(bitsPerEntryIn, arraySizeIn, null);
        }

        public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn, long[] longArrayIn)
        {
            this.arraySize = arraySizeIn;
            this.bitsPerEntry = bitsPerEntryIn;
            this.maxEntryValue = (1L << bitsPerEntryIn) - 1L;

            if (longArrayIn != null)
            {
                this.longArray = longArrayIn;
            }
            else
            {
                this.longArray = new long[(int) (((long) arraySizeIn * (long) bitsPerEntryIn + 63L) / 64L)];
            }
        }

        public void setAt(long index, int value)
        {
            long startOffset = index * (long) this.bitsPerEntry;
            int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
            int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
            int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64
            this.longArray[startArrIndex] = this.longArray[startArrIndex] & ~(this.maxEntryValue << startBitOffset) | ((long) value & this.maxEntryValue) << startBitOffset;

            if (startArrIndex != endArrIndex)
            {
                int endOffset = 64 - startBitOffset;
                int j1 = this.bitsPerEntry - endOffset;
                this.longArray[endArrIndex] = this.longArray[endArrIndex] >>> j1 << j1 | ((long) value & this.maxEntryValue) >> endOffset;
            }
        }

        public int getAt(long index)
        {
            long startOffset = index * (long) this.bitsPerEntry;
            int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
            int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
            int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

            if (startArrIndex == endArrIndex)
            {
                return (int) (this.longArray[startArrIndex] >>> startBitOffset & this.maxEntryValue);
            }
            else
            {
                int endOffset = 64 - startBitOffset;
                return (int) ((this.longArray[startArrIndex] >>> startBitOffset | this.longArray[endArrIndex] << endOffset) & this.maxEntryValue);
            }
        }

        public long[] getValueCounts()
        {
            long[] counts = new long[(int) this.maxEntryValue + 1];
            final long size = this.arraySize;

            for (long i = 0; i < size; ++i)
            {
                ++counts[this.getAt(i)];
            }

            return counts;
        }

        public long[] getBackingLongArray()
        {
            return this.longArray;
        }

        public long size()
        {
            return this.arraySize;
        }
    }
}
