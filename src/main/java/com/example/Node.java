package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.hash;

public final class Node implements Serializable {
    @Serial
    private static final long serialVersionUID = -8947112108914209515L;

    private final @NonNull String name;

    private final int depth;

    private final @Nullable Node parent;

    public Node(final @NonNull String name) {
        this(name, 0, null);
    }

    public Node(
            final @NonNull String name,
            final int depth,
            final @Nullable Node parent) {
        this.name = name;
        this.depth = depth;
        this.parent = parent;
    }

    public @NonNull String getName() {
        return name;
    }

    public int getDepth() {
        return depth;
    }

    public @Nullable Node getParent() {
        return parent;
    }

    public @NonNull Node newChild(final @NonNull String childName) {
        return new Node(childName, depth + 1, this);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj
               || obj instanceof final Node that
                  && depth == that.depth
                  && name.equals(that.name)
                  && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return hash(name, depth, parent);
    }

    @Override
    public String toString() {
        return format("{depth: %d, name: \"%s\", parent: %s}", depth, name, parent);
    }
}
