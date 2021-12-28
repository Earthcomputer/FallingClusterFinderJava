package net.earthcomputer.fallingclusterfinder;

public class HashMapFullException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
