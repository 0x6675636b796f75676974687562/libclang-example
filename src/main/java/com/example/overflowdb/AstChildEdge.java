package com.example.overflowdb;

import org.checkerframework.checker.nullness.qual.NonNull;
import overflowdb.Config;
import overflowdb.Edge;
import overflowdb.EdgeFactory;
import overflowdb.EdgeLayoutInformation;
import overflowdb.Graph;
import overflowdb.NodeRef;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

public final class AstChildEdge extends Edge {
    public static final String LABEL_E = "child";

    public static final Set<String> PROPERTY_KEYS = emptySet();

    public AstChildEdge(final Graph graph, final NodeRef outVertex, final NodeRef inVertex) {
        super(graph, LABEL_E, outVertex, inVertex, PROPERTY_KEYS);
    }

    public static final EdgeLayoutInformation LAYOUT_INFORMATION = new EdgeLayoutInformation(LABEL_E, PROPERTY_KEYS);

    /**
     * Used by the {@link Graph#open(Config, List, List)} factory method.
     *
     * @see Graph#open(Config, List, List)
     */
    public static final @NonNull EdgeFactory<AstChildEdge> FACTORY = new EdgeFactory<>() {
        @Override
        public @NonNull String forLabel() {
            return LABEL_E;
        }

        @Override
        public AstChildEdge createEdge(final Graph graph, final NodeRef outVertex, final NodeRef inVertex) {
            return new AstChildEdge(graph, outVertex, inVertex);
        }
    };
}
