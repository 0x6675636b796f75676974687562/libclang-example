package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.hash;

public final class AstNode implements Serializable {
    @Serial
    private static final long serialVersionUID = -8947112108914209515L;

    private final @NonNull String name;

    private final int depth;

    private final @NonNull List<@NonNull AstNode> children = new ArrayList<>();

    public AstNode(final @NonNull String name) {
        this(name, 0);
    }

    private AstNode(
            final @NonNull String name,
            final int depth) {
        this.name = name;
        this.depth = depth;
    }

    public @NonNull String getName() {
        return name;
    }

    public int getDepth() {
        return depth;
    }

    public @NonNull List<@NonNull AstNode> getChildren() {
        return unmodifiableList(children);
    }

    public @NonNull AstNode newChild(final @NonNull String childName) {
        final AstNode child = new AstNode(childName, depth + 1);
        children.add(child);
        return child;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj
               || obj instanceof final AstNode that
                  && depth == that.depth
                  && name.equals(that.name)
                  && children.equals(that.children);
    }

    @Override
    public int hashCode() {
        return hash(name, depth, children);
    }

    @Override
    public String toString() {
        return format("{depth: %d, name: \"%s\"}", depth, name);
    }
}
