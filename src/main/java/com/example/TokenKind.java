package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serial;

import static java.lang.String.format;
import static java.util.Objects.hash;

public final class TokenKind implements AstNodeKind {
    @Serial
    private static final long serialVersionUID = 5789516195359696025L;

    /**
     * @serial include
     */
    private final @NonNull String value;

    public TokenKind(final @NonNull String value) {
        this.value = value;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj
               || obj instanceof TokenKind that
                  && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return hash(value);
    }

    @Override
    public @NonNull String toString() {
        return format("%s(%s)", getClass().getSimpleName(), value);
    }
}
