package com.example.clang;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

public final class Utils {
    private Utils() {
        assert false;
    }

    @Contract("false, _ -> fail")
    public static void require(
            final boolean predicate,
            final @NonNull Supplier<@NonNull String> lazyMessage
    ) {
        if (!predicate) {
            throw new IllegalArgumentException(lazyMessage.get());
        }
    }

    @Contract("false, _ -> fail")
    public static void check(
            final boolean predicate,
            final @NonNull Supplier<@NonNull String> lazyMessage
    ) {
        if (!predicate) {
            throw new IllegalStateException(lazyMessage.get());
        }
    }
}
