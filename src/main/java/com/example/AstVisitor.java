package com.example;

import com.example.clang.ChildVisitResult;
import com.example.clang.CursorVisitor;
import com.example.clang.SourceLocation;
import com.example.clang.Tokens;
import org.bytedeco.llvm.clang.CXCursor;
import org.checkerframework.checker.nullness.qual.NonNull;

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

public final class AstVisitor implements CursorVisitor<AstNode> {
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

            System.out.printf("%s: depth = %d%n", location, parentAstNode.getDepth() + 1);

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
            return visitChildren(cursor, parentAstNode.newChild(cursorType))
                   ? BREAK
                   : CONTINUE;
        }
    }
}
