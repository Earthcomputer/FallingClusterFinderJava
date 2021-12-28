package net.earthcomputer.fallingclusterfinder;

import java.awt.Point;
import java.util.List;

public class GlassChunkCollidesException extends RuntimeException {
    public final List<Point> nearbyValid;

    public GlassChunkCollidesException(List<Point> nearbyValid) {
        this.nearbyValid = nearbyValid;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
