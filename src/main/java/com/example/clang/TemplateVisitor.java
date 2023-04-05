package com.example.clang;

import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXFile;
import org.bytedeco.llvm.clang.CXType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.example.clang.ChildVisitResult.BREAK;
import static com.example.clang.ChildVisitResult.CONTINUE;
import static org.bytedeco.llvm.global.clang.CXLinkage_External;
import static org.bytedeco.llvm.global.clang.CXLinkage_Internal;
import static org.bytedeco.llvm.global.clang.CXLinkage_Invalid;
import static org.bytedeco.llvm.global.clang.CXLinkage_NoLinkage;
import static org.bytedeco.llvm.global.clang.CXLinkage_UniqueExternal;
import static org.bytedeco.llvm.global.clang.clang_getCursorExtent;
import static org.bytedeco.llvm.global.clang.clang_getCursorKind;
import static org.bytedeco.llvm.global.clang.clang_getCursorKindSpelling;
import static org.bytedeco.llvm.global.clang.clang_getCursorLexicalParent;
import static org.bytedeco.llvm.global.clang.clang_getCursorLinkage;
import static org.bytedeco.llvm.global.clang.clang_getCursorSemanticParent;
import static org.bytedeco.llvm.global.clang.clang_getCursorSpelling;
import static org.bytedeco.llvm.global.clang.clang_getCursorType;
import static org.bytedeco.llvm.global.clang.clang_getCursorUSR;
import static org.bytedeco.llvm.global.clang.clang_getFileName;
import static org.bytedeco.llvm.global.clang.clang_getIncludedFile;
import static org.bytedeco.llvm.global.clang.clang_getTypeKindSpelling;
import static org.bytedeco.llvm.global.clang.clang_getTypeSpelling;
import static org.bytedeco.llvm.global.clang.clang_isAttribute;
import static org.bytedeco.llvm.global.clang.clang_isDeclaration;
import static org.bytedeco.llvm.global.clang.clang_isExpression;
import static org.bytedeco.llvm.global.clang.clang_isInvalid;
import static org.bytedeco.llvm.global.clang.clang_isPreprocessing;
import static org.bytedeco.llvm.global.clang.clang_isReference;
import static org.bytedeco.llvm.global.clang.clang_isStatement;
import static org.bytedeco.llvm.global.clang.clang_isTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_isUnexposed;

public final class TemplateVisitor implements CursorVisitor {
    @Override
    public @NonNull ChildVisitResult call(
            @NonNull final CXCursor cursor,
            @NonNull final CXCursor parent,
            final int depth
    ) {
        try (final SourceLocation location = new SourceLocation(cursor)) {
            if (!location.isFromMainFile()) {
                /*
                 * System include files.
                 */
                return CONTINUE;
            }

            System.out.printf("%s: depth = %d%n", location, depth);

            // XXX Use clang_getCursorExtent to get the range and tokenize the source if necessary.

            showCursorKind(cursor);
            showType(cursor);
            showSpelling(cursor);
            showUsr(cursor);
            showLinkage(cursor);
            showParent(cursor, parent);
            showIncludedFile(cursor);
            System.out.println();

            /*
             * Returning `RECURSE` here will have exactly the same effect as
             * calling `visitChildren`
             * (except for `depth` not being incremented).
             */
            return visitChildren(cursor, depth + 1)
                   ? BREAK
                   : CONTINUE;
        }
    }

    private static void showSpelling(final @NonNull CXCursor cursor) {
        final String cursorText = clang_getCursorSpelling(cursor).getString();
        System.out.printf("Text: %s%n", cursorText);

        try (final SourceRange range = new SourceRange(clang_getCursorExtent(cursor))) {
            final String cursorText2 = range.getText();
            System.out.printf("Text: %s%n", cursorText2);
        }
    }

    private static void showType(CXCursor cursor) {
        CXType type = clang_getCursorType(cursor);
        final String typeKind = clang_getTypeKindSpelling(type.kind()).getString();
        final String typeName = clang_getTypeSpelling(type).getString();
        if (typeName.isEmpty()) {
            System.out.printf("Type: %s%n", typeKind);
        } else {
            System.out.printf("Type: %s/%s%n", typeKind, typeName);
        }
    }

    private static void showLinkage(CXCursor cursor) {
        int linkage = clang_getCursorLinkage(cursor);
        String linkageName;
        switch (linkage) {
        case CXLinkage_Invalid:        linkageName = "Invalid"; break;
        case CXLinkage_NoLinkage:      linkageName = "NoLinkage"; break;
        case CXLinkage_Internal:       linkageName = "Internal"; break;
        case CXLinkage_UniqueExternal: linkageName = "UniqueExternal"; break;
        case CXLinkage_External:       linkageName = "External"; break;
        default:                       		 linkageName = "Unknown"; break;
        }
        System.out.printf("Linkage: %s\n", linkageName);
    }

    private static void showParent(CXCursor cursor, CXCursor parent) {
        CXCursor semaParent = clang_getCursorSemanticParent(cursor);
        CXCursor lexParent  = clang_getCursorLexicalParent(cursor);
        System.out.printf("Parent: parent:%s semantic:%s lexicial:%s\n",
                          clang_getCursorSpelling(parent).getString(),
                          clang_getCursorSpelling(semaParent).getString(),
                          clang_getCursorSpelling(lexParent).getString());
    }

    private static void showUsr(final @NonNull CXCursor cursor) {
        final String usr = clang_getCursorUSR(cursor).getString();
        if (!usr.isEmpty()) {
            System.out.printf("USR: %s%n", usr);
        }
    }

    private static void showCursorKind(CXCursor cursor) {
        int curKind  = clang_getCursorKind(cursor);

        String type;
        if (clang_isAttribute(curKind) == 1) type = "Attribute";
        else if (clang_isDeclaration(curKind) == 1) type = "Declaration";
        else if (clang_isExpression(curKind) == 1) type = "Expression";
        else if (clang_isInvalid(curKind) == 1) type = "Invalid";
        else if (clang_isPreprocessing(curKind) == 1) type = "Preprocessing";
        else if (clang_isReference(curKind) == 1) type = "Reference";
        else if (clang_isStatement(curKind) == 1) type = "Statement";
        else if (clang_isTranslationUnit(curKind) == 1) type = "TranslationUnit";
        else if (clang_isUnexposed(curKind) == 1) type = "Unexposed";
        else                               					  type = "Unknown";

        System.out.printf(
                "Cursor: %s/%s%n",
                type,
                clang_getCursorKindSpelling(curKind).getString()
        );
    }

    private static void showIncludedFile(CXCursor cursor) {
        final @Nullable CXFile included = clang_getIncludedFile(cursor);
        if (included == null) return;
        System.out.printf(" included file: %s\n", clang_getFileName(included).getString());
    }
}
