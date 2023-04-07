package com.example.overflowdb;

import overflowdb.Edge;
import overflowdb.EdgeFactory;
import overflowdb.EdgeLayoutInformation;
import overflowdb.Graph;
import overflowdb.NodeRef;

import java.util.Set;

import static java.util.Collections.emptySet;

public final class AstParentEdge extends Edge {
    public static final String LABEL = "parent";

    public static final Set<String> PROPERTY_KEYS = emptySet();

    public AstParentEdge(final Graph graph, final NodeRef outVertex, final NodeRef inVertex) {
        super(graph, LABEL, outVertex, inVertex, PROPERTY_KEYS);
    }

    public static final EdgeLayoutInformation LAYOUT_INFORMATION = new EdgeLayoutInformation(LABEL, PROPERTY_KEYS);

    public static final EdgeFactory<AstParentEdge> FACTORY = new EdgeFactory<>() {
        @Override
        public String forLabel() {
            return LABEL;
        }

        @Override
        public AstParentEdge createEdge(final Graph graph, final NodeRef outVertex, final NodeRef inVertex) {
            return new AstParentEdge(graph, outVertex, inVertex);
        }
    };
}
