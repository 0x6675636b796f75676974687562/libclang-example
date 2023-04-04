package com.example.clang;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXUnsavedFile;
import org.bytedeco.llvm.global.clang;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.example.clang.Utils.check;
import static java.lang.String.format;
import static org.bytedeco.llvm.global.clang.CXError_Success;
import static org.bytedeco.llvm.global.clang.CXToken_Comment;
import static org.bytedeco.llvm.global.clang.CXToken_Identifier;
import static org.bytedeco.llvm.global.clang.CXToken_Keyword;
import static org.bytedeco.llvm.global.clang.CXToken_Literal;
import static org.bytedeco.llvm.global.clang.CXToken_Punctuation;
import static org.bytedeco.llvm.global.clang.CXTranslationUnit_None;
import static org.bytedeco.llvm.global.clang.clang_createIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_getTokenExtent;
import static org.bytedeco.llvm.global.clang.clang_getTokenKind;
import static org.bytedeco.llvm.global.clang.clang_getTokenSpelling;
import static org.bytedeco.llvm.global.clang.clang_parseTranslationUnit2;

/**
 * https://github.com/sabottenda/libclang-sample/blob/master/Token/Tokenize.cc
 */
public final class Tokenizer {
	private static final Logger LOGGER = Logger.getLogger(Tokenizer.class.getName());

	private void tokenize(final @NonNull Path file) throws IOException {
		final String[] command_line_args = { };

		final CXIndex index = clang_createIndex(1, 0);

		final int num_unsaved_files = 0;
		final CXUnsavedFile unsaved_files = new CXUnsavedFile();
		final BytePointer source_filename = new BytePointer(file.toString());
		final PointerPointer<?> command_line_args_ptr = new PointerPointer<>(command_line_args);
		final CXTranslationUnit translationUnit = new CXTranslationUnit();
		checkError(
				clang_parseTranslationUnit2(
						index,
						source_filename,
						command_line_args_ptr,
						command_line_args.length,
						unsaved_files,
						num_unsaved_files,
						CXTranslationUnit_None,
						translationUnit
				)
		);

		try (final Tokens tokens = new Tokens(translationUnit, file)) {
			tokens.forEach(pair -> showToken(pair.getFirst(), pair.getSecond()));
		}

		clang_disposeTranslationUnit(translationUnit);
		clang_disposeIndex(index);
	}

	private void tokenize(final @NonNull String fileName) throws IOException, URISyntaxException {
		final URL resourceOrNull = getClass().getResource(fileName);
		if (resourceOrNull == null) {
			LOGGER.severe("File doesn't exist: " + fileName);
			return;
		}

		tokenize(Paths.get(resourceOrNull.toURI()));
	}

	public static void main(final @NonNull String @NonNull [] args) throws IOException, URISyntaxException {
		System.out.println("file.encoding: " + System.getProperty("file.encoding") + '/' + Charset.defaultCharset());
		new Tokenizer().tokenize("array-subscript.c" /*"sample1.cc"*/);
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

			System.out.printf("%s: %s, %d char(s): %s%n", tokenRange, tokenKind, tokenText.length(), tokenText);
		}
	}

	private static void checkError(final int errorCode) {
		if (errorCode != CXError_Success) {
			switch (errorCode) {
			case clang.CXError_InvalidArguments -> throw new RuntimeException("InvalidArguments");
			case clang.CXError_ASTReadError -> throw new RuntimeException("ASTReadError");
			case clang.CXError_Crashed -> throw new RuntimeException("Crashed");
			case clang.CXError_Failure -> throw new RuntimeException("Failure");
			}
		}
	}
}
