package com.example.clang;

import kotlin.Pair;
import kotlin.Triple;
import org.bytedeco.javacpp.annotation.ByVal;
import org.bytedeco.llvm.clang.CXFile;
import org.bytedeco.llvm.clang.CXSourceLocation;
import org.bytedeco.llvm.clang.CXSourceRange;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.nio.file.Files.size;
import static org.bytedeco.llvm.global.clang.clang_Range_isNull;
import static org.bytedeco.llvm.global.clang.clang_disposeTokens;
import static org.bytedeco.llvm.global.clang.clang_equalLocations;
import static org.bytedeco.llvm.global.clang.clang_getFile;
import static org.bytedeco.llvm.global.clang.clang_getLocationForOffset;
import static org.bytedeco.llvm.global.clang.clang_getNullLocation;
import static org.bytedeco.llvm.global.clang.clang_getRange;
import static org.bytedeco.llvm.global.clang.clang_tokenize;

public final class Tokens extends ClangAutoCloseable<CXToken> {
    private final @NonNull CXTranslationUnit translationUnit;

    private final int tokenCount;

    /**
     * @throws IOException if it's impossible to calculate the source range for
     *   the whole {@code file}.
     * @see #Tokens(CXTranslationUnit, CXSourceRange)
     */
    public Tokens(
            final @NonNull CXTranslationUnit translationUnit,
            final @NonNull Path file
    ) throws IOException {
        this(translationUnit, fileRangeOrThrow(translationUnit, file));
    }

    /**
     * @see #Tokens(CXTranslationUnit, Path)
     */
    public Tokens(
            final @NonNull CXTranslationUnit translationUnit,
            final @NonNull CXSourceRange range
    ) {
        super(new CXToken());
        this.translationUnit = translationUnit;
        final int tokenCount[] = new int[1];
        clang_tokenize(translationUnit, range, resource, tokenCount);
        this.tokenCount = tokenCount[0];
    }

    /**
     * @throws IllegalStateException if this buffer of tokens has already been
     *   closed.
     * @throws ConcurrentModificationException if two or more threads are
     *   concurrently invoking this method.
     */
    public void forEach(final @NonNull Consumer<@NonNull Pair<@NonNull CXTranslationUnit, @NonNull CXToken>> action) {
        forEachIndexed(triple -> action.accept(new Pair<>(triple.getSecond(), triple.getThird())));
    }

    /**
     * @throws IllegalStateException if this buffer of tokens has already been
     *   closed.
     * @throws ConcurrentModificationException if two or more threads are
     *   concurrently invoking this method.
     */
    public void forEachIndexed(final @NonNull Consumer<@NonNull Triple<@NonNull Integer, @NonNull CXTranslationUnit, @NonNull CXToken>> action) {
        if (isClosed()) {
            throw new IllegalStateException("Already closed");
        }

        flip();
        for (int index = 0; index < tokenCount; index++) {
            final long position = resource.position();
            if ((index == 0 && position != 0)
                || (index > 0 && position + 1 != index)) {
                throw new ConcurrentModificationException(
                        format(
                                "Expected buffer position: %d; actual position: %d",
                                index == 0 ? 0 : index - 1,
                                position
                        )
                );
            }

            action.accept(new Triple<>(index, translationUnit, resource.position(index)));
        }
    }

    @Contract(pure = true)
    public int getTokenCount() {
        return tokenCount;
    }

    @Contract(pure = true)
    public boolean isEmpty() {
        return getTokenCount() == 0;
    }

    /**
     * Re-sets the pointer if it has been incremented.
     */
    public void flip() {
        resource.position(0L);
    }

    @Override
    protected void closeInternal() {
        /*
         * Re-set the pointer before disposal, so that we don't overflow the
         * buffer.
         */
        flip();

        clang_disposeTokens(
                translationUnit,
                resource,
                tokenCount
        );
    }

    /**
     * @throws IOException if it's impossible to calculate the source range for
     *   the whole {@code file}.
     */
    private static @NonNull CXSourceRange fileRangeOrThrow(
            final @NonNull CXTranslationUnit translationUnit,
            final @NonNull Path file
    ) throws IOException {
        final CXSourceRange range = fileRange(translationUnit, file);
        if (range == null) {
            throw new IOException("Unable to get source range for " + file);
        }
        return range;
    }

    /**
     * @throws IOException if the size of the {@code file} can't be determined.
     */
    private static @Nullable CXSourceRange fileRange(
            final @NonNull CXTranslationUnit translationUnit,
            final @NonNull Path file
    ) throws IOException {
        final CXFile cxFile = clang_getFile(translationUnit, file.toString());
        final long fileSize = size(file);

        final CXSourceLocation begin  = clang_getLocationForOffset(translationUnit, cxFile, 0);
        final CXSourceLocation end = clang_getLocationForOffset(translationUnit, cxFile, toInt(fileSize));

        if (isNull(begin) || isNull(end)) {
            return null;
        }

        return getRange(begin, end);
    }

    private static int toInt(final long l) {
        if (l > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (l < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return (int) l;
    }

    private static @Nullable CXSourceRange getRange(
            @ByVal final @NonNull CXSourceLocation begin,
            @ByVal final @NonNull CXSourceLocation end) {
        final CXSourceRange range = clang_getRange(begin, end);

        if (isNull(range)) {
            return null;
        }

        return range;
    }

    private static boolean isNull(@ByVal final @NonNull CXSourceLocation location) {
        return clang_equalLocations(location, clang_getNullLocation()) == 1;
    }

    private static boolean isNull(@ByVal final @NonNull CXSourceRange range) {
        return clang_Range_isNull(range) == 1;
    }
}
