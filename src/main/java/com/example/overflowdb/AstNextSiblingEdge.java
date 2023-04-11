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

public final class AstNextSiblingEdge extends Edge {
    public static final String LABEL_E = "nextSibling";

    public static final Set<String> PROPERTY_KEYS = emptySet();

    public AstNextSiblingEdge(final Graph graph, final NodeRef outVertex, final NodeRef inVertex) {
        super(graph, LABEL_E, outVertex, inVertex, PROPERTY_KEYS);
    }

    public static final EdgeLayoutInformation LAYOUT_INFORMATION = new EdgeLayoutInformation(LABEL_E, PROPERTY_KEYS);

    /**
     * Used by the {@link Graph#open(Config, List, List)} factory method.
     *
     * @see Graph#open(Config, List, List)
     */
    public static final @NonNull EdgeFactory<AstNextSiblingEdge> FACTORY = new EdgeFactory<>() {
        @Override
        public @NonNull String forLabel() {
            return LABEL_E;
        }

        @Override
        public AstNextSiblingEdge createEdge(final Graph graph, final NodeRef outVertex, final NodeRef inVertex) {
            return new AstNextSiblingEdge(graph, outVertex, inVertex);
        }
    };
}
