package com.example.clang;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.clang.CXClientData;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXCursorVisitor;
import org.bytedeco.llvm.clang.CXFile;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXType;
import org.bytedeco.llvm.clang.CXUnsavedFile;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.bytedeco.llvm.global.clang.CXChildVisit_Continue;
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
import static org.bytedeco.llvm.global.clang.CXTranslationUnit_None;
import static org.bytedeco.llvm.global.clang.clang_createIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeTranslationUnit;
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
import static org.bytedeco.llvm.global.clang.clang_visitChildren;

/**
 * https://github.com/sabottenda/libclang-sample/blob/master/AST/ASTVisitor.cc
 */
public class ClangASTVisitorExample {
	private ClangASTVisitorExample() {
		assert false;
	}

	public static void main(String[] args) throws URISyntaxException {
		// Location of the *.cc file
		final URL resourceOrNull = ClangASTVisitorExample.class.getResource("array-subscript.c" /*"sample1.cc"*/);
		if (resourceOrNull == null) {
			System.out.println("File doesn't exist");
			return;
		}
		final Path file = Paths.get(resourceOrNull.toURI());

		String[] command_line_args = new String[] { };

		CXIndex index = clang_createIndex(1, 0);

		int num_unsaved_files = 0;
		CXUnsavedFile unsaved_files = new CXUnsavedFile();
		BytePointer source_filename = new BytePointer(file.toString());
		PointerPointer command_line_args_ptr = new PointerPointer(command_line_args);
		CXTranslationUnit translationUnit = new CXTranslationUnit();
		checkError(clang_parseTranslationUnit2(index, source_filename, command_line_args_ptr, command_line_args.length,
											   unsaved_files, num_unsaved_files, CXTranslationUnit_None, translationUnit));

		// traverse the elements
		CXClientData level = new CXClientData(new IntPointer(new int[] {0}));
		CXCursorVisitor visitor = new TemplateVisitor();
		CXCursor rootCursor  = clang_getTranslationUnitCursor(translationUnit);
		clang_visitChildren(rootCursor, visitor, level);

		// dispose all allocated data
		clang_disposeTranslationUnit(translationUnit);
		clang_disposeIndex(index);
	}

	/**
	 * Class with the callback method of the CXCursorVisitor.
	 */
	protected static class TemplateVisitor extends CXCursorVisitor {
		@Override
		public int call(
				final @NonNull CXCursor cursor,
				final @NonNull CXCursor parent,
				final @NonNull CXClientData clientData
		) {
			if (!new SourceLocation(cursor).isFromMainFile()) {
				return CXChildVisit_Continue;
			}

			final int depth = clientData.asByteBuffer().getInt();
			System.out.printf("Depth: %d\n", depth);

			showSpelling(cursor);
			showType(cursor);
			show_linkage(cursor);
			show_cursor_kind(cursor);
			show_parent(cursor, parent);
			showLocation(cursor);
			showUsr(cursor);
			show_included_file(cursor);
			System.out.println();

			final CXClientData nextDepth = new CXClientData(new IntPointer(new int[] {depth + 1 }));
			clang_visitChildren(cursor, this, nextDepth);

			return CXChildVisit_Continue;
		}


		protected void showSpelling(final @NonNull CXCursor cursor) {
			final String cursorText = clang_getCursorSpelling(cursor).getString();
			System.out.printf("Text: %s%n", cursorText);

			try (final SourceRange range = new SourceRange(clang_getCursorExtent(cursor))) {
				final String cursorText2 = range.getText();
				System.out.printf("Text: %s%n", cursorText2);
			}
		}

		protected void showType(CXCursor cursor) {
			CXType type = clang_getCursorType(cursor);
			System.out.printf("Type: %s\n", clang_getTypeSpelling(type).getString());
			System.out.printf("TypeKind: %s\n", clang_getTypeKindSpelling(type.kind()).getString());
		}

		protected void show_linkage(CXCursor cursor) {
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
			System.out.printf("  Linkage: %s\n", linkageName);
		}

		protected void show_parent(CXCursor cursor, CXCursor parent) {
			CXCursor semaParent = clang_getCursorSemanticParent(cursor);
			CXCursor lexParent  = clang_getCursorLexicalParent(cursor);
			System.out.printf("  Parent: parent:%s semantic:%s lexicial:%s\n",
					clang_getCursorSpelling(parent).getString(),
					clang_getCursorSpelling(semaParent).getString(),
					clang_getCursorSpelling(lexParent).getString());
		}

		protected void showLocation(final @NonNull CXCursor cursor) {
			final SourceLocation location = new SourceLocation(cursor);
			System.out.printf("%s%n", location);

			// XXX Use clang_getCursorExtent to get the range and tokenize the source if necessary.
		}

		protected void showUsr(final @NonNull CXCursor cursor) {
			final String usr = clang_getCursorUSR(cursor).getString();
			if (!usr.isEmpty()) {
				System.out.printf("USR: %s%n", usr);
			}
		}

		protected void show_cursor_kind(CXCursor cursor) {
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

			System.out.printf("  CursorKind: %s\n",  clang_getCursorKindSpelling(curKind).getString());
			System.out.printf("  CursorKindType: %s\n", type);
		}

		protected void show_included_file(CXCursor cursor) {
			CXFile included = clang_getIncludedFile(cursor);
			if (included == null) return;
			System.out.printf(" included file: %s\n", clang_getFileName(included).getString());
		}
	}

	/**
	 * Check for errors of the compilation process.
	 *
	 * @param errorCode
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
