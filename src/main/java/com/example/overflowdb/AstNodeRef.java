package com.example.overflowdb;

import com.example.AstNodeKind;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import overflowdb.Graph;
import overflowdb.NodeFactory;
import overflowdb.NodeRef;

import java.awt.Color;

public final class AstNodeRef extends NodeRef<AstNodeDb> {
    /**
     * Can be a {@code kind} returned by <em>Clang</em>.
     */
    public static final String LABEL_V = "astNode";

    public static final String LABEL = "label";

    public static final String KIND = "kind";

    public static final String COLOR = "color";

    public static final String RED = "r";

    public static final String GREEN = "g";

    public static final String BLUE = "b";

    public AstNodeRef(final Graph graph, final long id) {
        super(graph, id);
    }

    @Override
    public @NonNull String label() {
        return LABEL_V;
    }

    public @Nullable String getLabel() {
        return get().getLabel();
    }

    public @Nullable String getKind() {
        return get().getKind();
    }

    public @Nullable String getColor() {
        return get().getColor();
    }

    public int getRed() {
        return get().getRed();
    }

    public int getGreen() {
        return get().getGreen();
    }

    public int getBlue() {
        return get().getBlue();
    }

    public void addNextSibling(final @NonNull AstNodeRef nextSibling) {
        addEdgeImpl(AstNextSiblingEdge.LABEL_E, nextSibling);
    }

    public void addChild(final @NonNull AstNodeRef child) {
        addEdgeImpl(AstChildEdge.LABEL_E, child);
    }

    public AstNodeRef addChild(
            final @NonNull String label,
            final @Nullable AstNodeKind kind,
            final @Nullable Color color
    ) {
        final AstNodeRef child = (AstNodeRef) graph.addNode(
                LABEL_V,
                LABEL, label,
                KIND, kind,
                COLOR, color
        );

        addChild(child);

        return child;
    }

    public static final NodeFactory<AstNodeDb> FACTORY = new NodeFactory<>() {
        @Override
        public @NonNull String forLabel() {
            return LABEL_V;
        }

        @Override
        public AstNodeDb createNode(final @NonNull NodeRef<AstNodeDb> ref) {
            return new AstNodeDb(ref);
        }

        @Override
        public AstNodeRef createNodeRef(final Graph graph, final long id) {
            return new AstNodeRef(graph, id);
        }
    };
}
