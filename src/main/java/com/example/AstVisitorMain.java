package com.example;

import com.example.clang.CursorVisitor;
import com.example.clang.SourceLocation;
import com.example.clang.SourceRange;
import com.example.clang.Tokens;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXFile;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXString;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXType;
import org.bytedeco.llvm.clang.CXUnsavedFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.example.clang.ChildVisitResult.BREAK;
import static com.example.clang.ChildVisitResult.CONTINUE;
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
import static org.bytedeco.llvm.global.clang.CXTranslationUnit_None;
import static org.bytedeco.llvm.global.clang.clang_createIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeTranslationUnit;
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
import static org.bytedeco.llvm.global.clang.clang_getTranslationUnitCursor;
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
import static org.bytedeco.llvm.global.clang.clang_parseTranslationUnit2;

/**
 * @see <a href="https://github.com/sabottenda/libclang-sample/blob/master/AST/ASTVisitor.cc">ASTVisitor.cc</a>
 */
public final class AstVisitorMain {
	private AstVisitorMain() {
		assert false;
	}

	public static void main(final @NonNull String args @NonNull[]) throws URISyntaxException {
		if (args.length != 1) {
			System.err.printf("Usage: %s [FILE]%n", AstVisitorMain.class.getName());
			return;
		}

		final URL resourceOrNull = AstVisitorMain.class.getResource(args[0]);
		if (resourceOrNull == null) {
			System.out.println("File doesn't exist");
			return;
		}
		final Path file = Paths.get(resourceOrNull.toURI());

		final String[] commandLineArgs = { };

		final CXIndex index = clang_createIndex(1, 0);

		final int numUnsavedFiles = 0;
		final CXUnsavedFile unsavedFiles = new CXUnsavedFile();
		final BytePointer sourceFilename = new BytePointer(file.toString());
		final PointerPointer<?> commandLineArgsPtr = new PointerPointer<>(commandLineArgs);
		final CXTranslationUnit translationUnit = new CXTranslationUnit();
		checkError(
				clang_parseTranslationUnit2(
						index,
						sourceFilename,
						commandLineArgsPtr,
						commandLineArgs.length,
						unsavedFiles,
						numUnsavedFiles,
						CXTranslationUnit_None,
						translationUnit
				)
		);

		final CXCursor rootCursor = clang_getTranslationUnitCursor(translationUnit);
		CursorVisitor.<Node>from((visitor, cursor, parentCursor, parentNode) -> {
			try (final SourceLocation location = new SourceLocation(cursor)) {
				if (!location.isFromMainFile()) {
					/*
					 * System include files.
					 */
					return CONTINUE;
				}

				System.out.printf("%s: %s%n", location, parentNode);

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
				return visitor.visitChildren(cursor, parentNode.newChild(cursorType))
					   ? BREAK
					   : CONTINUE;
			}
		}).visitChildren(rootCursor, new Node("root"));

		clang_disposeTranslationUnit(translationUnit);
		clang_disposeIndex(index);
	}

	private static @NonNull String tokenKindSpelling(final int kind) {
		return switch (kind) {
			case CXToken_Punctuation -> "Punctuation";
			case CXToken_Keyword -> "Keyword";
			case CXToken_Identifier -> "Identifier";
			case CXToken_Literal -> "Literal";
			case CXToken_Comment -> "Comment";
			default -> throw new RuntimeException("Unknown token kind: " + kind);
		};
	}

	private static void showToken(
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

			System.out.printf("\t%s: %s, %d char(s): %s%n", tokenRange, tokenKind, tokenText.length(), tokenText);
		}
	}

	private static void showSpelling(final @NonNull CXCursor cursor) {
		final String cursorText = clang_getCursorSpelling(cursor).getString();
		if (!cursorText.isEmpty()) {
			System.out.printf("Text: %s%n", cursorText);
		}

		try (final SourceRange range = new SourceRange(cursor)) {
			final String cursorText2 = range.getText();
			System.out.printf("Text: %s%n", cursorText2);
		}
	}

	private static @NonNull String getType(final CXCursor cursor) {
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

	private static void showLinkage(final CXCursor cursor) {
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

	private static void showParent(final CXCursor cursor, final CXCursor parent) {
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

	private static void showUsr(final @NonNull CXCursor cursor) {
		final String usr = clang_getCursorUSR(cursor).getString();
		if (!usr.isEmpty()) {
			System.out.printf("USR: %s%n", usr);
		}
	}

	private static void showCursorKind(final CXCursor cursor) {
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

	private static void showIncludedFile(final CXCursor cursor) {
		final @Nullable CXFile included = clang_getIncludedFile(cursor);
		if (included == null) {
			return;
		}
		System.out.printf(" included file: %s%n", clang_getFileName(included).getString());
	}

	/**
	 * Check for errors of the compilation process.
	 */
	private static void checkError(final int errorCode) {
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
