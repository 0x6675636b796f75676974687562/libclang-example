package com.example.overflowdb;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import overflowdb.NodeDb;
import overflowdb.NodeLayoutInformation;
import overflowdb.NodeRef;

import java.util.List;
import java.util.Set;

import static com.example.overflowdb.AstNodeRef.LABEL;

public final class AstNodeDb extends NodeDb {
    private String label;

    AstNodeDb(final NodeRef<AstNodeDb> nodeRef) {
        super(nodeRef);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public NodeLayoutInformation layoutInformation() {
        return new NodeLayoutInformation(
                AstNodeRef.LABEL_V,
                Set.of(LABEL),
                List.of(AstParentEdge.LAYOUT_INFORMATION),
                List.of(AstParentEdge.LAYOUT_INFORMATION));
    }

    @Override
    public @Nullable Object property(final @NonNull String key) {
        return switch (key) {
            case LABEL -> label;
            default -> null;
        };
    }

    @Override
    protected void updateSpecificProperty(final @NonNull String key, final @Nullable Object value) {
        switch (key) {
        case LABEL -> label = (String) value;
        }
    }

    @Override
    protected void removeSpecificProperty(final @NonNull String key) {
        updateSpecificProperty(key, null);
    }
}
