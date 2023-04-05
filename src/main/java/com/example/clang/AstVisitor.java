package com.example.clang;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXUnsavedFile;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.bytedeco.llvm.global.clang.CXError_ASTReadError;
import static org.bytedeco.llvm.global.clang.CXError_Crashed;
import static org.bytedeco.llvm.global.clang.CXError_Failure;
import static org.bytedeco.llvm.global.clang.CXError_InvalidArguments;
import static org.bytedeco.llvm.global.clang.CXError_Success;
import static org.bytedeco.llvm.global.clang.CXTranslationUnit_None;
import static org.bytedeco.llvm.global.clang.clang_createIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_getTranslationUnitCursor;
import static org.bytedeco.llvm.global.clang.clang_parseTranslationUnit2;

/**
 * https://github.com/sabottenda/libclang-sample/blob/master/AST/ASTVisitor.cc
 */
public class AstVisitor {
	private AstVisitor() {
		assert false;
	}

	public static void main(final String[] args) throws URISyntaxException {
		final URL resourceOrNull = AstVisitor.class.getResource("array-subscript.c" /*"sample1.cc"*/);
		if (resourceOrNull == null) {
			System.out.println("File doesn't exist");
			return;
		}
		final Path file = Paths.get(resourceOrNull.toURI());

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

		// traverse the elements
		final CXCursor rootCursor = clang_getTranslationUnitCursor(translationUnit);
		new TemplateVisitor().visitChildren(rootCursor);

		// dispose all allocated data
		clang_disposeTranslationUnit(translationUnit);
		clang_disposeIndex(index);
	}

	/**
	 * Check for errors of the compilation process.
	 */
	protected static void checkError(final int errorCode) {
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
