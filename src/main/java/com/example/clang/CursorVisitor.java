package com.example.clang;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.llvm.clang.CXClientData;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXCursorVisitor;
import org.checkerframework.checker.nullness.qual.NonNull;

import static org.bytedeco.llvm.global.clang.clang_visitChildren;

@FunctionalInterface
public interface CursorVisitor {
    /**
     * Invoked for each cursor found during traversal.
     *
     * <p>
     * Will be invoked for each cursor found by {@code clang_visitCursorChildren()}.
     * </p>
     *
     * @param cursor the cursor being visited.
     * @param parent the parent visitor for that cursor.
     * @param depth the client data provided to {@code clang_visitCursorChildren()}.
     * @return one of the {@link ChildVisitResult} values to direct
     *   {@code clang_visitCursorChildren()}.
     * @see CXCursorVisitor#call(CXCursor, CXCursor, CXClientData)
     */
    @NonNull ChildVisitResult call(
            final @NonNull CXCursor cursor,
            final @NonNull CXCursor parent,
            final int depth
    );

    /**
     * Visits the children of a particular cursor.
     *
     * <p>
     * This function visits all the direct children of the given cursor,
     * invoking the given visitor function with the cursors of each
     * visited child. The traversal may be recursive, if the visitor returns
     * {@link ChildVisitResult#RECURSE}. The traversal may also be ended
     * prematurely, if the visitor returns {@link ChildVisitResult#BREAK}.
     * </p>
     *
     * @param parent the cursor whose child may be visited. All kinds of cursors
     *               can be visited, including invalid cursors (which, by
     *               definition, have no children).
     * @return {@code true} if the traversal was terminated prematurely by the
     *   visitor returning {@link ChildVisitResult#BREAK}.
     * @see #visitChildren(CXCursor, int)
     */
    default boolean visitChildren(final @NonNull CXCursor parent) {
        return visitChildren(parent, 0);
    }

    /**
     * Visits the children of a particular cursor.
     *
     * <p>
     * This function visits all the direct children of the given cursor,
     * invoking the given visitor function with the cursors of each
     * visited child. The traversal may be recursive, if the visitor returns
     * {@link ChildVisitResult#RECURSE}. The traversal may also be ended
     * prematurely, if the visitor returns {@link ChildVisitResult#BREAK}.
     * </p>
     *
     * @param parent the cursor whose child may be visited. All kinds of cursors
     *               can be visited, including invalid cursors (which, by
     *               definition, have no children).
     * @param depth the depth from the root of the AST.
     * @return {@code true} if the traversal was terminated prematurely by the
     *   visitor returning {@link ChildVisitResult#BREAK}.
     * @see #visitChildren(CXCursor)
     */
    default boolean visitChildren(
            final @NonNull CXCursor parent,
            final int depth
    ) {
        try (final CXCursorVisitor visitor = asCxCursorVisitor()) {
            try (final CXClientData clientData = new CXClientData(new IntPointer(new int[] {depth}))) {
                return clang_visitChildren(parent, visitor, clientData) != 0;
            }
        }
    }

    private CXCursorVisitor asCxCursorVisitor() {
        return new CXCursorVisitor() {
            @Override
            public int call(
                    final @NonNull CXCursor cursor,
                    final @NonNull CXCursor parent,
                    @NonNull final CXClientData clientData
            ) {
                final int depth = clientData.asByteBuffer().getInt();

                return CursorVisitor.this.call(
                        cursor,
                        parent,
                        depth
                ).ordinal();
            }
        };
    }
}
