package net.earthcomputer.fallingclusterfinder;

import java.util.Locale;

public enum Direction {
    NORTH("North"), SOUTH("South"), WEST("West"), EAST("East");

    private final String name;
    Direction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String internalName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isXAxis() {
        return this == EAST || this == WEST;
    }

    public Direction[] getOrthogonalDirections() {
        if (isXAxis()) {
            return new Direction[] { NORTH, SOUTH };
        } else {
            return new Direction[] { WEST, EAST };
        }
    }

    public Direction getOpposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            case EAST:
                return WEST;
            default:
                throw new IllegalStateException("Unknown direction: " + this);
        }
    }
}
