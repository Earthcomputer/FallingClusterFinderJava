package net.earthcomputer.fallingclusterfinder;

public class CanceledException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
