package com.example.clang;

import kotlin.jvm.functions.Function4;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.clang.CXClientData;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXCursorVisitor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.util.Objects.requireNonNull;
import static org.bytedeco.llvm.global.clang.clang_visitChildren;

@FunctionalInterface
public interface CursorVisitor<T extends Serializable> {
    /**
     * Invoked for each cursor found during traversal.
     *
     * <p>
     * Will be invoked for each cursor found by {@code clang_visitCursorChildren()}.
     * </p>
     *
     * @param cursor the cursor being visited.
     * @param parent the parent visitor for that cursor.
     * @param clientData the client data provided to {@code clang_visitCursorChildren()}.
     * @return one of the {@link ChildVisitResult} values to direct
     *   {@code clang_visitCursorChildren()}.
     * @see CXCursorVisitor#call(CXCursor, CXCursor, CXClientData)
     */
    @NonNull ChildVisitResult call(
            final @NonNull CXCursor cursor,
            final @NonNull CXCursor parent,
            final @NonNull T clientData
    );

    /**
     * Visits the children of a particular cursor.
     *
     * <p>
     * This function visits all the direct children of the given cursor,
     * invoking the given visitor function with the cursors of each
     * visited child. The traversal may be recursive, if the visitor returns
     * {@link ChildVisitResult#RECURSE}. The traversal may also be ended
     * prematurely, if the visitor returns {@link ChildVisitResult#BREAK}.
     * </p>
     *
     * @param parent the cursor whose child may be visited. All kinds of cursors
     *               can be visited, including invalid cursors (which, by
     *               definition, have no children).
     * @param clientData the client data provided to {@code clang_visitCursorChildren()}.
     * @return {@code true} if the traversal was terminated prematurely by the
     *   visitor returning {@link ChildVisitResult#BREAK}.
     */
    default boolean visitChildren(
            final @NonNull CXCursor parent,
            final @NonNull T clientData
    ) {
        try (final CXCursorVisitor visitor = asCxCursorVisitor()) {
            try (final CXClientData rawClientData = toClientData(clientData)) {
                return clang_visitChildren(parent, visitor, rawClientData) != 0;
            }
        }
    }

    private CXCursorVisitor asCxCursorVisitor() {
        return new CXCursorVisitor() {
            @Override
            public int call(
                    final @NonNull CXCursor cursor,
                    final @NonNull CXCursor parent,
                    final @NonNull CXClientData clientData
            ) {
                return CursorVisitor.this.call(
                        cursor,
                        parent,
                        fromClientData(clientData)
                ).ordinal();
            }
        };
    }

    /**
     * Creates a new <em>stateless</em> visitor from a lambda.
     *
     * <p>
     * It will be possible to pass data to child invocations
     * (so that child AST nodes may see their parents), but not vice versa:
     * the data passed across a native stack frame will be serialized and
     * de-serialized, so any state changes will be lost when a child invocation
     * returns. The only workaround is to use a custom {@code readResolve()}
     * method when de-serializing data.
     * </p>
     *
     * <p>
     * Alternatively, you can create a <em>stateful</em> visitor by subclassing
     * {@code CursorVisitor}.
     * </p>
     *
     * @param block the lambda which constitutes the visitor body.
     * @return the new stateless visitor.
     * @param <T> the type of data (e.g.: such as recursion depth) to pass to
     *           child invocations of this visitor.
     *           Only makes sense if the visitor manually invokes
     *           {@link #visitChildren(CXCursor, Serializable)}
     *           and returns {@link ChildVisitResult#CONTINUE} rather than
     *           {@link ChildVisitResult#RECURSE}.
     * @see #visitChildren(CXCursor, Serializable)
     * @see ChildVisitResult#CONTINUE
     * @see ChildVisitResult#RECURSE
     */
    static <T extends @NonNull Serializable> @NonNull CursorVisitor<T> from(
            final @NonNull Function4<? super @NonNull CursorVisitor<T>, ? super @NonNull CXCursor, ? super @NonNull CXCursor, ? super @NonNull T, @NonNull ChildVisitResult> block
    ) {
        return new CursorVisitor<>() {
            @Override
            public @NonNull ChildVisitResult call(
                    final @NonNull CXCursor cursor,
                    final @NonNull CXCursor parent,
                    final T clientData
            ) {
                return block.invoke(this, cursor, parent, clientData);
            }
        };
    }

    /**
     * Writes {@code obj} to a byte buffer.
     * The first 4 bytes in the buffer will hold the length of the data section.
     * This is necessary, because type information (such as length) can't pass
     * through a native stack frame,
     * and all we get on the JVM side by default is a byte array pointer
     * (8 bytes on a 64-bit JVM, 4 bytes on a 32-bit JVM) without any length
     * information.
     *
     * @param obj the object to write to a byte buffer.
     * @return the byte buffer, which contains the length information as its 4
     *   bytes, followed by the serialized object.
     */
    private static @NonNull ByteBuffer toByteBuffer(final @NonNull Serializable obj) {
        final ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();

        try (final ObjectOutput out = new ObjectOutputStream(arrayOut)) {
            out.writeObject(obj);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        final byte bytes[] = arrayOut.toByteArray();

        /*
         * The order is big-endian by default.
         */
        final ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        return buffer;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> @NonNull T fromByteBuffer(final @NonNull ByteBuffer data) {
        try (final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(asArray(data)))) {
            return requireNonNull((T) in.readObject());
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        } catch (final ClassNotFoundException cnfe) {
            throw (NoClassDefFoundError) new NoClassDefFoundError(cnfe.getMessage()).initCause(cnfe);
        }
    }

    private static byte @NonNull[] asArray(final @NonNull ByteBuffer data) {
        if (data.hasArray()) {
            return data.array();
        }

        data.rewind();
        final byte bytes[] = new byte[data.remaining()];
        data.get(bytes);
        return bytes;
    }

    private static @NonNull CXClientData toClientData(final @NonNull Serializable obj) {
        return new CXClientData(new BytePointer(toByteBuffer(obj)));
    }

    private static <T extends @NonNull Serializable> @NonNull T fromClientData(final @NonNull CXClientData clientData) {
        /*
         * The size of JVM `int`, in bytes.
         */
        final int offset = 4;

        final BytePointer dataPointer = new BytePointer(clientData);

        /*
         * On x86, the byte-order of the direct buffer passed from the native
         * stack frame will be little-endian by default.
         *
         * A call to `Pointer.capacity()` will set both the capacity and the limit.
         */
        dataPointer.capacity(offset);
        final int length = dataPointer.asByteBuffer().order(BIG_ENDIAN).getInt();

        /*
         * Will set both the capacity and the limit.
         */
        dataPointer.capacity(offset + length);
        return fromByteBuffer(dataPointer.asByteBuffer().order(BIG_ENDIAN).slice(offset, length));
    }
}
