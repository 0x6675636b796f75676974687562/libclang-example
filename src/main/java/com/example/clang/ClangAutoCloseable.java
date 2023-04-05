package com.example.clang;

import org.bytedeco.javacpp.Pointer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public abstract class ClangAutoCloseable<T extends Pointer> implements AutoCloseable {
    protected final @NonNull T resource;

    private boolean closed = false;

    private final @NonNull List<@NonNull ClangAutoCloseable<?>> childResources =  new ArrayList<>();

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

    protected final void addChildResource(final @NonNull ClangAutoCloseable<?> childResource) {
        childResources.add(childResource);
    }

    @Override
    public final void close() {
        if (!isClosed()) {
            closeInternal();
            childResources.forEach(ClangAutoCloseable::close);
            resource.close();
            closed = true;
        }
    }
}
