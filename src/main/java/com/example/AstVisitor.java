package com.example;

import com.example.clang.ChildVisitResult;
import com.example.clang.CursorVisitor;
import com.example.clang.SourceLocation;
import com.example.clang.Tokens;
import org.bytedeco.llvm.clang.CXCursor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Supplier;

import static com.example.AstVisitorUtils.getType;
import static com.example.AstVisitorUtils.showCursorKind;
import static com.example.AstVisitorUtils.showIncludedFile;
import static com.example.AstVisitorUtils.showLinkage;
import static com.example.AstVisitorUtils.showParent;
import static com.example.AstVisitorUtils.showSpelling;
import static com.example.AstVisitorUtils.showToken;
import static com.example.AstVisitorUtils.showUsr;
import static com.example.clang.ChildVisitResult.BREAK;
import static com.example.clang.ChildVisitResult.CONTINUE;
import static com.example.clang.Utils.check;
import static java.lang.String.format;

public final class AstVisitor implements CursorVisitor<AstNode>, ParentNodeAware {
    /**
     * The mutable state of this <em>stateful</em> visitor,
     * changed for each child invocation.
     *
     * @see #withNewParent(AstNode, Supplier)
     */
    private @NonNull AstNode parentNode;

    public AstVisitor(final @NonNull AstNode parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @param parentAstNode the same as {@link #parentNode},
     *                      but passed across native stack frames as a parameter
     *                      rather than a mutable visitor state.
     * @see #parentNode
     */
    @Override
    public @NonNull ChildVisitResult call(
            final @NonNull CXCursor cursor,
            final @NonNull CXCursor parentCursor,
            final @NonNull AstNode parentAstNode
    ) {
        try (final SourceLocation location = new SourceLocation(cursor)) {
            if (!location.isFromMainFile()) {
                /*
                 * System include files.
                 */
                return CONTINUE;
            }

            /*
             * Make sure that serialized data is passed correctly across a
             * native stack frame.
             */
            check(
                    parentNode.getName().equals(parentAstNode.getName()),
                    () -> format("%s != %s", parentNode.getName(), parentAstNode.getName())
            );
            check(
                    parentNode.getDepth() == parentAstNode.getDepth(),
                    () -> format("%d != %d", parentNode.getDepth(), parentAstNode.getDepth())
            );

            System.out.printf("%s: depth = %d%n", location, parentNode.getDepth() + 1);

            showCursorKind(cursor);
            final String cursorType = getType(cursor);
            System.out.println("Type: " + cursorType);
            try (final Tokens tokens = new Tokens(cursor)) {
                tokens.forEach(pair -> showToken(pair.getFirst(), pair.getSecond()));
            }
            showSpelling(cursor);
            showUsr(cursor);
            showLinkage(cursor);
            showParent(cursor, parentCursor);
            showIncludedFile(cursor);
            System.out.println();

            /*
             * Returning `RECURSE` here will have exactly the same effect as
             * calling `visitChildren`
             * (except for client data not being updated).
             */
            final AstNode newParent = parentNode.newChild(cursorType);
            return withNewParent(newParent, () -> visitChildren(cursor, newParent))
                   ? BREAK
                   : CONTINUE;
        }
    }

    @Override
    public @NonNull AstNode getParentNode() {
        return parentNode;
    }

    @Override
    public void setParentNode(final @NonNull AstNode parentNode) {
        this.parentNode = parentNode;
    }
}
