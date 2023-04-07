package com.example;

import com.example.clang.SourceRange;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.hash;

public final class AstNode implements Serializable {
    @Serial
    private static final long serialVersionUID = -8947112108914209515L;

    /**
     * @serial include
     */
    private final @NonNull String text;

    /**
     * @serial include
     */
    private final int depth;

    /**
     * @serial include
     */
    private final @Nullable String range;

    /**
     * @serial include
     */
    private final @Nullable AstNodeKind kind;

    /**
     * @serial include
     */
    private final @NonNull List<@NonNull AstNode> children = new ArrayList<>();

    public AstNode(final @NonNull String text) {
        this(text, 0, null, null);
    }

    private AstNode(
            final @NonNull String text,
            final int depth,
            final @Nullable SourceRange range,
            final @Nullable AstNodeKind kind) {
        this.text = text;
        this.depth = depth;
        this.range = range == null ? null : range.toString();
        this.kind = kind;
    }

    public @NonNull String getText() {
        return text;
    }

    public int getDepth() {
        return depth;
    }

    public @Nullable String getRange() {
        return range;
    }

    public @Nullable AstNodeKind getKind() {
        return kind;
    }

    public @NonNull List<@NonNull AstNode> getChildren() {
        return unmodifiableList(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public @NonNull AstNode addChild(
            final @NonNull String childText,
            final @Nullable SourceRange childRange,
            final @Nullable AstNodeKind childKind
    ) {
        final AstNode child = new AstNode(
                childText,
                depth + 1,
                childRange,
                childKind
        );
        children.add(child);
        return child;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj
               || obj instanceof final AstNode that
                  && depth == that.depth
                  && text.equals(that.text)
                  && Objects.equals(range, that.range)
                  && Objects.equals(kind, that.kind)
                  && children.equals(that.children);
    }

    @Override
    public int hashCode() {
        return hash(text, depth, range, kind, children);
    }

    @Override
    public String toString() {
        return format("{depth: %d, text: \"%s\"}", depth, text);
    }
}
