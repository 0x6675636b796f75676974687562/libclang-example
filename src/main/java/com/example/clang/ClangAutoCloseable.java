package com.example.clang;

import org.bytedeco.javacpp.Pointer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;

public abstract class ClangAutoCloseable<T extends Pointer> implements AutoCloseable {
    protected final @NonNull T resource;

    private boolean closed = false;

    protected ClangAutoCloseable(final @NonNull T resource) {
        this.resource = resource;
    }

    @Contract(pure = true)
    public final boolean isClosed() {
        return closed;
    }

    protected void closeInternal() {
        // empty
    }

    @Override
    public final void close() {
        if (!isClosed()) {
            closeInternal();
            resource.close();
            closed = true;
        }
    }
}
