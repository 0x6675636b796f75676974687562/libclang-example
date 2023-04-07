package com.example;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXUnsavedFile;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.example.AstVisitorUtils.checkError;
import static org.bytedeco.llvm.global.clang.CXTranslationUnit_None;
import static org.bytedeco.llvm.global.clang.clang_createIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_getTranslationUnitCursor;
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
		new AstVisitor().visitChildren(rootCursor, new AstNode(file.getFileName().toString()));

		clang_disposeTranslationUnit(translationUnit);
		clang_disposeIndex(index);
	}
}
