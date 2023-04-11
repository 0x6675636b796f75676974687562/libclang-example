package com.example;

import com.example.overflowdb.AstChildEdge;
import com.example.overflowdb.AstNextSiblingEdge;
import com.example.overflowdb.AstNodeRef;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXUnsavedFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import overflowdb.Config;
import overflowdb.Graph;
import overflowdb.formats.dot.DotExporter;
import overflowdb.formats.graphml.GraphMLExporter;

import java.awt.Color;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.example.AstVisitorUtils.checkError;
import static java.nio.file.Files.deleteIfExists;
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

	public static void main(final @NonNull String args @NonNull[]) throws URISyntaxException, IOException {
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
		final String fileName = file.getFileName().toString();
		final AstNode rootAstNode = new AstNode(fileName);
		new AstVisitor(rootAstNode).visitChildren(rootCursor, rootAstNode);

		clang_disposeTranslationUnit(translationUnit);
		clang_disposeIndex(index);

		final Path graphStorage = Path.of(fileName + ".h2");
		deleteIfExists(graphStorage);
		final Config config = Config.withDefaults()
									.withSerializationStatsEnabled()
									.withStorageLocation(graphStorage);
		try (final Graph graph = Graph.open(
				config,
				List.of(AstNodeRef.FACTORY),
				List.of(AstChildEdge.FACTORY, AstNextSiblingEdge.FACTORY)
		)) {
			addRoot(graph, rootAstNode);

			GraphMLExporter.runExport(graph, Path.of(fileName + ".graphml").toAbsolutePath());
			DotExporter.runExport(graph, Path.of(fileName + ".dot").toAbsolutePath());
		}
	}

	private static void addRoot(
			final @NonNull Graph graph,
			final @NonNull AstNode rootAstNode
	) {
		final AstNodeRef graphRoot = (AstNodeRef) graph.addNode(
				AstNodeRef.LABEL_V,
				AstNodeRef.LABEL,
				rootAstNode.getText(),
				AstNodeRef.COLOR,
				Color.RED
		);

		AstNodeRef previousSibling = null;
		for (final AstNode astChild : rootAstNode.getChildren()) {
			previousSibling = addChildRecursively(graphRoot, astChild, previousSibling);
		}
	}

	private static @NonNull AstNodeRef addChildRecursively(
			final @NonNull AstNodeRef graphParent,
			final @NonNull AstNode astChild,
			final @Nullable AstNodeRef previousSibling
	) {
		final AstNodeRef graphChild = addChild(graphParent, astChild, previousSibling);

		AstNodeRef graphSubChild = null;
		for (final AstNode astSubChild : astChild.getChildren()) {
			graphSubChild = addChildRecursively(graphChild, astSubChild, graphSubChild);
		}

		return graphChild;
	}

	private static @NonNull AstNodeRef addChild(
			final @NonNull AstNodeRef graphParent,
			final @NonNull AstNode astChild,
			final @Nullable AstNodeRef previousSibling
	) {
		final Color color;
		final AstNodeKind kind = astChild.getKind();
		if (kind instanceof TokenKind) {
			color = Color.CYAN;
		} else {
			color = Color.GREEN;
		}

		final AstNodeRef graphChild = graphParent.addChild(
				astChild.getText(),
				kind,
				color
		);

		if (previousSibling != null) {
			previousSibling.addNextSibling(graphChild);
		}

		return graphChild;
	}
}
