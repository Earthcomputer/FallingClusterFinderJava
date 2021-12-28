package net.earthcomputer.fallingclusterfinder;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Point;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class FallingClusterFinder {
    private static FallingClusterGui gui;
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static volatile boolean running = false;
    private static volatile boolean canceled = false;
    private static final Object RUNNING_CANCEL_LOCK = new Object();

    private static void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // keep default laf
        }
    }

    public static void main(String[] args) {
        setLookAndFeel();
        gui = new FallingClusterGui();
        JFrame frame = new JFrame("Falling Cluster Finder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new JScrollPane(gui.getMainPanel()));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void start() {
        int hashSize = gui.getHashSize();
        int renderDistance = gui.getRenderDistance();
        OptionalInt spawnBlockX = gui.getSpawnX();
        OptionalInt spawnBlockZ = gui.getSpawnZ();
        OptionalInt glassChunkX = gui.getGlassX();
        OptionalInt glassChunkZ = gui.getGlassZ();
        OptionalInt unloadSearchChunkX = gui.getUnloadChunkSearchFromX();
        OptionalInt unloadSearchChunkZ = gui.getUnloadChunkSearchFromZ();
        int rectangleWidth = gui.getRectangleWidth();
        int clusterChunks = gui.getClusterChunkCount();
        OptionalInt clusterSearchCx = gui.getClusterChunksSearchFromX();
        OptionalInt clusterSearchCz = gui.getClusterChunksSearchFromZ();
        int minSearch = gui.getSearchLimit();
        Optional<List<FallingClusterGui.PermaloaderLine>> permaloaderLines = gui.getPermaloaderLines();

        if (!spawnBlockX.isPresent()
                || !spawnBlockZ.isPresent()
                || !glassChunkX.isPresent()
                || !glassChunkZ.isPresent()
                || !unloadSearchChunkX.isPresent()
                || !unloadSearchChunkZ.isPresent()
                || !clusterSearchCx.isPresent()
                || !clusterSearchCz.isPresent()
                || !permaloaderLines.isPresent()) {
            return;
        }

        executor.execute(() -> {
            synchronized (RUNNING_CANCEL_LOCK) {
                if (canceled) {
                    canceled = false;
                    return;
                }
                running = true;
            }
            SwingUtilities.invokeLater(() -> gui.setRunning(true));

            try {
                find(
                        hashSize,
                        renderDistance,
                        new Point(spawnBlockX.getAsInt(), spawnBlockZ.getAsInt()),
                        new Point(glassChunkX.getAsInt(), glassChunkZ.getAsInt()),
                        new Point(unloadSearchChunkX.getAsInt(), unloadSearchChunkZ.getAsInt()),
                        rectangleWidth,
                        clusterChunks,
                        new Point(clusterSearchCx.getAsInt(), clusterSearchCz.getAsInt()),
                        minSearch,
                        permaloaderLines.get()
                );
            } catch (CanceledException e) {
                // ignore
            } catch (HashMapFullException e) {
                SwingUtilities.invokeLater(gui::addHashMapFullError);
            } catch (GlassChunkCollidesException e) {
                SwingUtilities.invokeLater(() -> gui.addGlassChunkCollidesError(e.nearbyValid));
            } finally {
                synchronized (RUNNING_CANCEL_LOCK) {
                    running = false;
                    canceled = false;
                }
                SwingUtilities.invokeLater(() -> gui.setRunning(false));
            }
        });
    }

    private static void find(int hashSize, int renderDistance, Point spawnPos, Point glassChunk, Point unloadSearchOrigin, int rectangleWidth, int clusterChunks, Point clusterSearchOrigin, int minSearch, List<FallingClusterGui.PermaloaderLine> permaloaders) {
        OpenHashMap preloadedChunks = new OpenHashMap(hashSize);
        OpenHashMap illegalChunks = new OpenHashMap(hashSize);
        prefillHashMap(hashSize, spawnPos, glassChunk, permaloaders, preloadedChunks, illegalChunks);

        int glassHash = OpenHashMap.hash(glassChunk, hashSize);

        int sizeBefore = clusterChunks;
        sizeBefore = accountForExistingClusterChunks(preloadedChunks, glassHash, sizeBefore);
        sizeBefore += preloadedChunks.size;
        int hashSizeBefore = getMinHashSize(sizeBefore);

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                illegalChunks.insert(new Point(glassChunk.x + dx, glassChunk.y + dz));
            }
        }

        Point unloadChunk = findGlassHashChunk(glassHash, hashSize, unloadSearchOrigin, 0, illegalChunks, chunk -> {
            for (int dx = 0; dx < renderDistance*2+2; dx++) {
                for (int dz = 0; dz < renderDistance*2+2; dz++) {
                    if ((dx != 0 || dz != 0) && (dx != renderDistance*2+1 || dz != renderDistance*2+1)) {
                        Point otherChunk = new Point(chunk.x + dx, chunk.y + dz);
                        if (OpenHashMap.hash(otherChunk, hashSize) == glassHash) {
                            return false;
                        }
                        if (illegalChunks.contains(otherChunk) || !isPermaloaderProtected(otherChunk)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        });

        for (int dx = 0; dx < renderDistance*2+2; dx++) {
            for (int dz = 0; dz < renderDistance*2+2; dz++) {
                if (dx != renderDistance*2+1 || dz != renderDistance*2+1) {
                    Point otherChunk = new Point(unloadChunk.x + dx, unloadChunk.y + dz);
                    illegalChunks.insert(otherChunk);
                }
            }
        }
        SwingUtilities.invokeLater(() -> gui.addChunkOutput("Unload chunk", unloadChunk));
        illegalChunks.insert(unloadChunk);

        checkCanceled();

        findClusterChunks(hashSize, clusterSearchOrigin, rectangleWidth, clusterChunks, preloadedChunks, glassHash, illegalChunks, minSearch);

        int hashSizeAfter = getMinHashSize(illegalChunks.size);

        if (hashSizeBefore != hashSizeAfter) {
            int sizeBefore_f = sizeBefore;
            SwingUtilities.invokeLater(() -> gui.addRehashWarning(sizeBefore_f, hashSizeAfter));
        }
    }

    private static void prefillHashMap(int hashSize, Point spawnPos, Point glassChunk, List<FallingClusterGui.PermaloaderLine> permaloaders, OpenHashMap preloadedChunks, OpenHashMap illegalChunks) {
        Point xSpawnChunksRange = getSpawnChunksRange(spawnPos.x);
        Point zSpawnChunksRange = getSpawnChunksRange(spawnPos.y);
        for (int spawnX = xSpawnChunksRange.x; spawnX <= xSpawnChunksRange.y; spawnX++) {
            for (int spawnZ = zSpawnChunksRange.x; spawnZ <= zSpawnChunksRange.y; spawnZ++) {
                Point spawnChunk = new Point(spawnX, spawnZ);
                preloadedChunks.insert(spawnChunk);
                illegalChunks.insert(spawnChunk);
            }
        }

        for (FallingClusterGui.PermaloaderLine permaloader : permaloaders) {
            int permaloaderLength = Math.abs(permaloader.getX1() - permaloader.getX2());
            for (int i = 0; i <= permaloaderLength; i++) {
                int chunkX = permaloader.getX1() + i * (permaloader.getX2() - permaloader.getX1()) / permaloaderLength;
                int chunkZ = permaloader.getZ1() + i * (permaloader.getZ2() - permaloader.getZ1()) / permaloaderLength;
                illegalChunks.insert(new Point(chunkX, chunkZ));
            }
        }

        if (glassChunk.x == 0 && glassChunk.y == 0 || preloadedChunks.vec[OpenHashMap.hash(glassChunk, hashSize)] != null) {
            int radius = 1;
            List<Point> found = new ArrayList<>();
            while (radius % 5 != 0 || found.isEmpty()) {
                for (int dx = -radius; dx <= radius; dx++) {
                    Point chunk = new Point(glassChunk.x + dx, glassChunk.y + radius - Math.abs(dx));
                    if (preloadedChunks.vec[OpenHashMap.hash(chunk, hashSize)] == null) {
                        found.add(chunk);
                    }
                    if (Math.abs(dx) != radius) {
                        chunk = new Point(glassChunk.x + dx, glassChunk.y - radius + Math.abs(dx));
                        if (preloadedChunks.vec[OpenHashMap.hash(chunk, hashSize)] == null) {
                            found.add(chunk);
                        }
                    }
                }
                radius++;
            }
            throw new GlassChunkCollidesException(found);
        }
    }

    private static Point getSpawnChunksRange(int coord) {
        int begin = (coord - 128 - 8 + 15) >> 4;
        int end = (coord + 128 - 8) >> 4;
        return new Point(begin, end);
    }

    private static int accountForExistingClusterChunks(OpenHashMap preloadedChunks, int glassHash, int numClusterChunks) {
        int index = (glassHash + numClusterChunks + 1) & preloadedChunks.mask;
        while (index != glassHash) {
            if (preloadedChunks.vec[index] != null) {
                numClusterChunks--;
            }
            if (index == 0) {
                index = preloadedChunks.mask;
            } else {
                index--;
            }
        }
        return numClusterChunks;
    }

    private static int getMinHashSize(int sizeBefore) {
        int nextPowerOf2 = Integer.highestOneBit(sizeBefore - 1) << 1;
        if (sizeBefore >= nextPowerOf2 * 3 / 4) {
            return nextPowerOf2 << 1;
        } else {
            return nextPowerOf2;
        }
    }

    private static Point findGlassHashChunk(int glassHash, int hashSize, Point origin, int minRadius, OpenHashMap illegalChunks, Predicate<Point> predicate) {
        int radius = minRadius;
        while (true) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int dz = (radius - Math.abs(dx)) * sign;
                    Point chunk = new Point(origin.x + dx, origin.y + dz);
                    if (!illegalChunks.contains(chunk) && isPermaloaderProtected(chunk) && OpenHashMap.hash(chunk, hashSize) == glassHash && predicate.test(chunk)) {
                        return chunk;
                    }
                }
            }
            radius++;
        }
    }

    private static void findClusterChunks(int hashSize, Point origin, int rectangleWidth, int numClusterChunks, OpenHashMap preloadedChunks, int glassHash, OpenHashMap illegalChunks, int minSearch) {
        int radius = 1;
        Point bestRectangleOrigin = null;
        Point bestRectangleSize = new Point(Short.MAX_VALUE, Short.MAX_VALUE);
        List<Point> bestClusterChunks = null;
        int searched = 0;
        int reconsidered = 0;
        while (reconsidered == 0 || searched < minSearch) {
            // TODO: progress bar
            for (int dx = -radius; dx <= radius; dx++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int dz = (radius - Math.abs(dx)) * sign;
                    Point rectangleOrigin = new Point(origin.x + dx, origin.y + dz);
                    int length = 1;
                    List<Point> clusterChunks = new ArrayList<>();
                    while (true) {
                        checkCanceled();
                        Point rectangleSize = new Point(rectangleWidth, length);
                        RectangleCheckResult result = checkRectangle(rectangleOrigin, rectangleSize, preloadedChunks, illegalChunks, glassHash, hashSize, numClusterChunks, clusterChunks);
                        if (result == RectangleCheckResult.FAILED) {
                            break;
                        } else if (result == RectangleCheckResult.SUCCESS) {
                            searched++;
                            if (rectangleSize.x * rectangleSize.y < bestRectangleSize.x * bestRectangleSize.y) {
                                bestRectangleOrigin = rectangleOrigin;
                                bestRectangleSize = rectangleSize;
                                bestClusterChunks = clusterChunks;
                                reconsidered++;
                            }
                            break;
                        }
                        length++;
                    }
                    length = 1;
                    clusterChunks = new ArrayList<>();
                    while (true) {
                        checkCanceled();
                        //noinspection SuspiciousNameCombination
                        Point rectangleSize = new Point(length, rectangleWidth);
                        RectangleCheckResult result = checkRectangle(rectangleOrigin, rectangleSize, preloadedChunks, illegalChunks, glassHash, hashSize, numClusterChunks, clusterChunks);
                        if (result == RectangleCheckResult.FAILED) {
                            break;
                        } else if (result == RectangleCheckResult.SUCCESS) {
                            searched++;
                            if (rectangleSize.x * rectangleSize.y < bestRectangleSize.x * bestRectangleSize.y) {
                                bestRectangleOrigin = rectangleOrigin;
                                bestRectangleSize = rectangleSize;
                                bestClusterChunks = clusterChunks;
                                reconsidered++;
                            }
                            break;
                        }
                        length++;
                    }
                }
            }
            radius++;
        }

        // test for rehash
        OpenHashMap testHashMap = new OpenHashMap(illegalChunks);
        for (Point chunk : bestClusterChunks) {
            testHashMap.insert(chunk);
        }

        bestClusterChunks.sort(Comparator.<Point>comparingInt(p -> p.x).thenComparingInt(p -> p.y));
        Point bestRectangleOrigin_f = bestRectangleOrigin;
        Point bestRectangleSize_f = bestRectangleSize;
        List<Point> bestClusterChunks_f = bestClusterChunks;
        SwingUtilities.invokeLater(() -> gui.addClusterChunksOutput(bestRectangleOrigin_f, bestRectangleSize_f, glassHash, hashSize, bestClusterChunks_f));
    }

    private static RectangleCheckResult checkRectangle(Point rectanglePos, Point rectangleSize, OpenHashMap preloadedChunks, OpenHashMap illegalChunks, int glassHash, int hashSize, int numClusterChunks, List<Point> clusterChunks) {
        numClusterChunks = accountForExistingClusterChunks(preloadedChunks, glassHash, numClusterChunks);

        List<Map.Entry<Integer, Point>> hashes = new ArrayList<>(rectangleSize.x * rectangleSize.y);
        for (int dx = 0; dx < rectangleSize.x; dx++) {
            for (int dz = 0; dz < rectangleSize.y; dz++) {
                Point chunk = new Point(rectanglePos.x + dx, rectanglePos.y + dz);
                if (illegalChunks.contains(chunk) || !isPermaloaderProtected(chunk)) {
                    continue;
                }
                int hash = OpenHashMap.hash(chunk, hashSize);
                if (hash != glassHash) {
                    int transformedHash = (hash + hashSize - glassHash) & preloadedChunks.mask;
                    hashes.add(new AbstractMap.SimpleEntry<>(transformedHash, chunk));
                }
            }
        }
        if (hashes.size() < numClusterChunks) {
            return RectangleCheckResult.CONTINUE;
        }

        hashes.sort(Map.Entry.comparingByKey());
        for (int i = 0; i < numClusterChunks; i++) {
            if (hashes.get(i).getKey() > i + 1) {
                return RectangleCheckResult.CONTINUE;
            }
        }
        for (int i = 0; i < numClusterChunks; i++) {
            clusterChunks.add(hashes.get(i).getValue());
        }
        return RectangleCheckResult.SUCCESS;
    }

    private static boolean isPermaloaderProtected(Point chunk) {
        int permaloaderHash = chunk.x ^ chunk.y;
        return permaloaderHash != 0 && permaloaderHash != -1;
    }

    private static void checkCanceled() {
        if (canceled) {
            throw new CanceledException();
        }
    }

    public static void cancel() {
        synchronized (RUNNING_CANCEL_LOCK) {
            if (running) {
                canceled = true;
            }
        }
    }

    private enum RectangleCheckResult {
        SUCCESS, CONTINUE, FAILED
    }
}
