package com.example;

import com.example.clang.SourceRange;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXFile;
import org.bytedeco.llvm.clang.CXString;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.stream.Stream;

import static com.example.clang.Utils.check;
import static java.lang.String.format;
import static org.bytedeco.llvm.global.clang.CXError_ASTReadError;
import static org.bytedeco.llvm.global.clang.CXError_Crashed;
import static org.bytedeco.llvm.global.clang.CXError_Failure;
import static org.bytedeco.llvm.global.clang.CXError_InvalidArguments;
import static org.bytedeco.llvm.global.clang.CXError_Success;
import static org.bytedeco.llvm.global.clang.CXLinkage_External;
import static org.bytedeco.llvm.global.clang.CXLinkage_Internal;
import static org.bytedeco.llvm.global.clang.CXLinkage_Invalid;
import static org.bytedeco.llvm.global.clang.CXLinkage_NoLinkage;
import static org.bytedeco.llvm.global.clang.CXLinkage_UniqueExternal;
import static org.bytedeco.llvm.global.clang.CXToken_Comment;
import static org.bytedeco.llvm.global.clang.CXToken_Identifier;
import static org.bytedeco.llvm.global.clang.CXToken_Keyword;
import static org.bytedeco.llvm.global.clang.CXToken_Literal;
import static org.bytedeco.llvm.global.clang.CXToken_Punctuation;
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
import static org.bytedeco.llvm.global.clang.clang_getTokenExtent;
import static org.bytedeco.llvm.global.clang.clang_getTokenKind;
import static org.bytedeco.llvm.global.clang.clang_getTokenSpelling;
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

public final class AstVisitorUtils {
    private AstVisitorUtils() {
        assert false;
    }

    public static @NonNull String tokenKindSpelling(final int kind) {
        return switch (kind) {
            case CXToken_Punctuation -> "Punctuation";
            case CXToken_Keyword -> "Keyword";
            case CXToken_Identifier -> "Identifier";
            case CXToken_Literal -> "Literal";
            case CXToken_Comment -> "Comment";
            default -> throw new RuntimeException("Unknown token kind: " + kind);
        };
    }

    public static void addToken(
            final @NonNull AstNode parentNode,
            final @NonNull CXTranslationUnit translationUnit,
            final @NonNull CXToken token
    ) {
        try (final SourceRange tokenRange = new SourceRange(clang_getTokenExtent(translationUnit, token))) {
            final String tokenKind = tokenKindSpelling(clang_getTokenKind(token));
            final String tokenText = clang_getTokenSpelling(translationUnit, token).getString();
            final String tokenText2 = tokenRange.getText();

            check(
                    tokenText.equals(tokenText2),
                    () -> format("%s != %s", tokenText, tokenText2)
            );

            parentNode.addChild(tokenText, tokenRange, new TokenKind(tokenKind));
        }
    }

    public static void showSpelling(final @NonNull CXCursor cursor) {
        final String cursorText = clang_getCursorSpelling(cursor).getString();
        if (!cursorText.isEmpty()) {
            System.out.printf("Text: %s%n", cursorText);
        }

        try (final SourceRange range = new SourceRange(cursor)) {
            final String cursorText2 = range.getText();
            System.out.printf("Text: %s%n", cursorText2);
        }
    }

    public static @NonNull String getType(final CXCursor cursor) {
        final CXType type = clang_getCursorType(cursor);
        final String typeKind;
        try (final CXString typeKindRaw = clang_getTypeKindSpelling(type.kind())) {
            typeKind = typeKindRaw.getString();
        }
        final String typeName = clang_getTypeSpelling(type).getString();
        return typeName.isEmpty()
               ? typeKind
               : format("%s/%s", typeKind, typeName);
    }

    public static void showLinkage(final CXCursor cursor) {
        final int linkage = clang_getCursorLinkage(cursor);
        final String linkageName = switch (linkage) {
            case CXLinkage_Invalid -> "Invalid";
            case CXLinkage_NoLinkage -> "NoLinkage";
            case CXLinkage_Internal -> "Internal";
            case CXLinkage_UniqueExternal -> "UniqueExternal";
            case CXLinkage_External -> "External";
            default -> "Unknown";
        };
        System.out.printf("Linkage: %s%n", linkageName);
    }

    public static void showParent(final CXCursor cursor, final CXCursor parent) {
        final CXCursor semaParent = clang_getCursorSemanticParent(cursor);
        final CXCursor lexParent  = clang_getCursorLexicalParent(cursor);
        final String parentText = clang_getCursorSpelling(parent).getString();
        final String semanticParent = clang_getCursorSpelling(semaParent).getString();
        final String lexicalParent = clang_getCursorSpelling(lexParent).getString();
        if (Stream.of(parentText, semanticParent, lexicalParent).anyMatch(text -> !text.isEmpty())) {
            System.out.println("Parent:");
        }
        if (!parentText.isEmpty()) {
            System.out.printf("\tParent: %s%n", parentText);
        }
        if (!semanticParent.isEmpty()) {
            System.out.printf("\tSemantic parent: %s%n", semanticParent);
        }
        if (!lexicalParent.isEmpty()) {
            System.out.printf("\tLexical parent: %s%n", lexicalParent);
        }
    }

    public static void showUsr(final @NonNull CXCursor cursor) {
        final String usr = clang_getCursorUSR(cursor).getString();
        if (!usr.isEmpty()) {
            System.out.printf("USR: %s%n", usr);
        }
    }

    public static void showCursorKind(final CXCursor cursor) {
        final int curKind  = clang_getCursorKind(cursor);

        final String type;
        if (clang_isAttribute(curKind) == 1) {
            type = "Attribute";
        } else if (clang_isDeclaration(curKind) == 1) {
            type = "Declaration";
        } else if (clang_isExpression(curKind) == 1) {
            type = "Expression";
        } else if (clang_isInvalid(curKind) == 1) {
            type = "Invalid";
        } else if (clang_isPreprocessing(curKind) == 1) {
            type = "Preprocessing";
        } else if (clang_isReference(curKind) == 1) {
            type = "Reference";
        } else if (clang_isStatement(curKind) == 1) {
            type = "Statement";
        } else if (clang_isTranslationUnit(curKind) == 1) {
            type = "TranslationUnit";
        } else if (clang_isUnexposed(curKind) == 1) {
            type = "Unexposed";
        } else {
            type = "Unknown";
        }

        try (final CXString cursorKindSpelling = clang_getCursorKindSpelling(curKind)) {
            System.out.printf(
                    "Cursor: %s/%s%n",
                    type,
                    cursorKindSpelling.getString()
            );
        }
    }

    public static void showIncludedFile(final CXCursor cursor) {
        final @Nullable CXFile included = clang_getIncludedFile(cursor);
        if (included == null) {
            return;
        }
        System.out.printf(" included file: %s%n", clang_getFileName(included).getString());
    }

    /**
     * Check for errors of the compilation process.
     */
    public static void checkError(final int errorCode) {
        if (errorCode != CXError_Success) {
            switch (errorCode) {
            case CXError_InvalidArguments -> throw new RuntimeException("InvalidArguments");
            case CXError_ASTReadError -> throw new RuntimeException("ASTReadError");
            case CXError_Crashed -> throw new RuntimeException("Crashed");
            case CXError_Failure -> throw new RuntimeException("Failure");
            }
        }
    }
}
