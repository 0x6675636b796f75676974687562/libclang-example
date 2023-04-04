package com.example.clang;

import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXFile;
import org.bytedeco.llvm.clang.CXSourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.file.Path;

import static java.lang.String.format;
import static org.bytedeco.llvm.global.clang.clang_Location_isFromMainFile;
import static org.bytedeco.llvm.global.clang.clang_getCursorLocation;
import static org.bytedeco.llvm.global.clang.clang_getFileName;
import static org.bytedeco.llvm.global.clang.clang_getSpellingLocation;

public final class SourceLocation extends ClangAutoCloseable<CXSourceLocation> {
    private final @NonNull Path file;

    private final int line;

    private final int column;

    private final int offsetBytes;

    public SourceLocation(final @NonNull CXCursor cursor) {
        this(clang_getCursorLocation(cursor));
    }

    public SourceLocation(final @NonNull CXSourceLocation location) {
        super(location);

        final CXFile file = new CXFile();
        final int[] line = new int[1];
        final int[] column = new int[1];
        final int[] offsetBytes = new int[1];

        clang_getSpellingLocation(resource, file, line, column, offsetBytes);

        this.file = Path.of(clang_getFileName(file).getString());
        this.line = line[0];
        this.column = column[0];
        this.offsetBytes = offsetBytes[0];
    }

    public @NonNull Path getFile() {
        return file;
    }

    /**
     * @return the line number (1-based).
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the column number (1-based).
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return the offset from the beginning of the file, in bytes.
     */
    public int getOffsetBytes() {
        return offsetBytes;
    }

    /**
     * Returns {@code true} if the given source location is in the main file of
     * the corresponding translation unit.
     *
     * @return {@code true} if the given source location is in the main file of
     *   the corresponding translation unit.
     */
    public boolean isFromMainFile() {
        return clang_Location_isFromMainFile(resource) != 0;
    }

    @Override
    public @NonNull String toString() {
        return format("%s:%d:%d", getFile(), getLine(), getColumn());
    }
}
