package com.example.clang;

import org.bytedeco.llvm.clang.CXSourceRange;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static com.example.clang.Utils.check;
import static com.example.clang.Utils.require;
import static java.lang.String.format;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.StandardOpenOption.READ;
import static org.bytedeco.llvm.global.clang.clang_getRangeEnd;
import static org.bytedeco.llvm.global.clang.clang_getRangeStart;

public final class SourceRange extends ClangAutoCloseable<CXSourceRange> {
    public SourceRange(final @NonNull CXSourceRange range) {
        super(range);
    }

    /**
     * @return the fragment of the file content which corresponds to this range.
     * @see #getText(Charset)
     */
    public @NonNull String getText() {
        return getText(defaultCharset());
    }

    /**
     * @return the fragment of the file content which corresponds to this range.
     * @see #getText()
     */
    public @NonNull String getText(final @NonNull Charset charset) {
        final SourceLocation beginLocation = new SourceLocation(clang_getRangeStart(resource));
        final SourceLocation endLocation = new SourceLocation(clang_getRangeEnd(resource));

        final Path beginFile = beginLocation.getFile();
        final int beginOffsetBytes = beginLocation.getOffsetBytes();

        final Path endFile = endLocation.getFile();
        final int endOffsetBytes = endLocation.getOffsetBytes();

        check(
                beginOffsetBytes >= 0,
                () -> "Begin offset is negative: " + beginOffsetBytes
        );
        check(
                endOffsetBytes >= 0,
                () -> "End offset is negative: " + endOffsetBytes
        );
        check(
                endOffsetBytes >= beginOffsetBytes,
                () -> format(
                        "Range length should be non-negative: [%d, %d)",
                        beginOffsetBytes,
                        endOffsetBytes
                )
        );

        check(
                beginFile.equals(endFile),
                () -> format(
                        "Begin and end locations point to different files: %s != %s",
                        beginFile,
                        endFile
                )
        );

        if (beginOffsetBytes == endOffsetBytes) {
            return "";
        }

        final int size = endOffsetBytes - beginOffsetBytes;

        try (final FileChannel channel = FileChannel.open(beginFile, READ)) {
            final MappedByteBuffer mappedBuffer = channel.map(READ_ONLY, beginOffsetBytes, size);
            return charset.decode(mappedBuffer).toString();
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Returns the string representation of this range, in either of the
     * following forms:
     *
     * <ul>
     *     <li>{@code sourcefile:line1:column1-line2:column2}</li>
     *     <li>{@code sourcefile:line:column1-column2}</li>
     *     <li>{@code sourcefile:line:column}</li>
     *     <li>{@code file1:line1.column1-file2:line2.column2}</li>
     * </ul>
     *
     * @return the string representation of this range.
     */
    @Override
    public @NonNull String toString() {
        final SourceLocation beginLocation = new SourceLocation(clang_getRangeStart(resource));
        final SourceLocation endLocation = new SourceLocation(clang_getRangeEnd(resource));

        final Path beginFile = beginLocation.getFile();
        final int beginLine = beginLocation.getLine();
        final int beginColumn = beginLocation.getColumn();

        final Path endFile = endLocation.getFile();
        final int endLine = endLocation.getLine();
        final int endColumn = endLocation.getColumn();

        if (beginFile.equals(endFile)) {
            /*-
             * sourcefile:line1:column1-line2:column2
             * sourcefile:line:column1-column2
             * sourcefile:line:column
             */
            return format(
                    "%s:%s",
                    beginFile,
                    toString(
                            beginLine,
                            beginColumn,
                            endLine,
                            endColumn
                    )
            );
        }

        /*
         * file1:line1:column1-file2:line2:column2
         */
        return format(
                "%s-%s",
                beginLocation,
                endLocation
        );
    }

    private static @NonNull String toString(
            final int beginLine,
            final int beginColumn,
            final int endLine,
            final int endColumn
    ) {
        require(beginLine > 0, () -> "Begin line is less than 1: " + beginLine);
        require(beginColumn > 0, () -> "Begin column is less than 1: " + beginColumn);
        require(endLine > 0, () -> "End line is less than 1: " + endLine);
        require(endColumn > 0, () -> "End column is less than 1: " + endColumn);

        if (beginLine == endLine) {
            if (beginColumn == endColumn || endColumn - beginColumn == 1) {
                return format("%d:%d", beginLine, beginColumn);
            }

            return format("%d:%d-%d", beginLine, beginColumn, endColumn);
        }

        return format("%d:%d-%d:%d", beginLine, beginColumn, endLine, endColumn);
    }
}
