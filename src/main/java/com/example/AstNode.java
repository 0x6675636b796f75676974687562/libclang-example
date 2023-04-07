package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.hash;

public final class AstNode implements Serializable {
    @Serial
    private static final long serialVersionUID = -8947112108914209515L;

    private final @NonNull String name;

    private final int depth;

    private final @Nullable AstNode parent;

    public AstNode(final @NonNull String name) {
        this(name, 0, null);
    }

    public AstNode(
            final @NonNull String name,
            final int depth,
            final @Nullable AstNode parent) {
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

    public @Nullable AstNode getParent() {
        return parent;
    }

    public @NonNull AstNode newChild(final @NonNull String childName) {
        return new AstNode(childName, depth + 1, this);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj
               || obj instanceof final AstNode that
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
