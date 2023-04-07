package com.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

public interface ParentNodeAware {
    @NonNull AstNode getParentNode();

    void setParentNode(final @NonNull AstNode parentNode);

    /**
     * @see #withNewParent(AstNode, Runnable)
     */
    default <T> T withNewParent(
            final @NonNull AstNode newParent,
            final @NonNull Supplier<T> lambda
    ) {
        /*
         * Save parent node before visiting children.
         */
        final @NonNull AstNode oldParent = getParentNode();
        setParentNode(newParent);
        try {
            return lambda.get();
        } finally {
            /*
             * Restore parent node.
             */
            setParentNode(oldParent);
        }
    }

    /**
     * @see #withNewParent(AstNode, Supplier)
     */
    default void withNewParent(
            final @NonNull AstNode newParent,
            final @NonNull Runnable lambda
    ) {
        this.<@Nullable Void>withNewParent(newParent, () -> {
            lambda.run();
            return null;
        });
    }
}
