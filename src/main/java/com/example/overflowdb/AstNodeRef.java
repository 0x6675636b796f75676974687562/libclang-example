package com.example.overflowdb;

import org.checkerframework.checker.nullness.qual.NonNull;
import overflowdb.Graph;
import overflowdb.NodeFactory;
import overflowdb.NodeRef;

public final class AstNodeRef extends NodeRef<AstNodeDb> {
    /**
     * Can be a {@code kind} returned by <em>Clang</em>.
     */
    public static final String LABEL_V = "astNode";

    public static final String LABEL = "label";

    public AstNodeRef(final Graph graph, final long id) {
        super(graph, id);
    }

    @Override
    public String label() {
        return LABEL_V;
    }

    public String getLabel() {
        return get().getLabel();
    }

    public static final NodeFactory<AstNodeDb> FACTORY = new NodeFactory<>() {
        @Override
        public String forLabel() {
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
