package com.example.javaw.lang;

/**
 * An object that may hold resources until it is closed.
 * The {@link #close()} method is invoked automatically on objects managed by a
 * {@code try}-with-resources statement.
 */
public interface AutoCloseable {

    /**
     * Closes this resource, relinquishing any underlying resources.
     *
     * @throws Exception if this resource cannot be closed
     */
    void close() throws Exception;
}
