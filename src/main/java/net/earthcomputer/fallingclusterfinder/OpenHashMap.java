package net.earthcomputer.fallingclusterfinder;

import java.awt.Point;

public final class OpenHashMap {
    public final Point[] vec;
    public final int mask;
    public int size;
    public final int maxFill;
    public boolean containsZero;

    public OpenHashMap(int hashSize) {
        vec = new Point[hashSize];
        mask = hashSize - 1;
        size = 0;
        maxFill = hashSize * 3 / 4;
        containsZero = false;
    }

    public OpenHashMap(OpenHashMap other) {
        vec = other.vec.clone();
        mask = other.mask;
        size = other.size;
        maxFill = other.maxFill;
        containsZero = other.containsZero;
    }

    public boolean contains(Point pos) {
        if (pos.x == 0 && pos.y == 0) {
            return containsZero;
        }
        int i = hash(pos, vec.length);
        while (vec[i] != null) {
            if (vec[i].equals(pos)) {
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    public void insert(Point pos) {
        if (pos.x == 0 && pos.y == 0) {
            if (!containsZero) {
                if (size >= maxFill) {
                    throw new HashMapFullException();
                }
                size++;
                containsZero = true;
            }
            return;
        }

        int i = hash(pos, vec.length);
        while (vec[i] != null) {
            if (vec[i].equals(pos)) {
                return;
            }
            i = (i + 1) & mask;
        }
        if (size >= maxFill) {
            throw new HashMapFullException();
        }
        vec[i] = pos;
        size++;
    }

    public static int hash(Point pos, int hashSize) {
        long l = ((long) pos.y << 32) | pos.x & 0xffffffffL;
        long hashed = l * 0x9E3779B97F4A7C15L;
        hashed ^= hashed >> 32;
        hashed ^= hashed >> 16;
        return (int) (hashed & (hashSize - 1));
    }
}
