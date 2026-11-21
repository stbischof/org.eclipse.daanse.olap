/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.segmentcache.common;

import java.nio.charset.StandardCharsets;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentHeader;
import org.eclipse.daanse.olap.util.ByteString;

/**
 * Serialization shared by the external segment stores.
 *
 * The wire key is the header's deterministic unique-id string — it contains
 * the schemaChecksum, so a catalog change means a new key and never a stale
 * hit. Header and body serialize separately: header listing warms the index
 * at manager start and must not pull cell data. Values travel as
 * {@code byte[]}, so the remote store needs no Daanse classpath.
 *
 * Every value is framed 'DSEG' + version + flags + payload: readers detect
 * foreign or old bytes deterministically (the stores degrade them to a
 * miss). The payload's first byte names its encoding — headers and the four
 * body shapes travel in a manual tagged form, everything else in the
 * filtered Java-serialized form — and large payloads are deflate-compressed
 * per entry.
 */
public final class SegmentCodec {

    private SegmentCodec() {
    }

    /**
     * Typed key inside the JVM; {@link #value()} is the only place it becomes
     * the wire string. The full header must never be a distributed map key:
     * its lazy fields make the same header serialize differently depending on
     * when they materialize.
     */
    public record SegmentKey(String value) {
        public SegmentKey {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("empty segment key");
            }
        }
    }

    /** A stamped entry, decoded: origin node plus the value. */
    public record StampedValue(String nodeId, Object value) {
    }

    /** A pub/sub event, decoded: origin node, event kind, header frame bytes. */
    public record EventValue(String nodeId, boolean created, byte[] header) {
    }

    /**
     * Deterministic store key: checksum and fact table prefix the unique id,
     * so one star's inventory is a plain prefix match in every store.
     */
    public static SegmentKey key(SegmentHeader header) {
        return new SegmentKey(keyPrefix(header.schemaChecksum, header.rolapStarFactTableName)
                + header.getUniqueID().toString());
    }

    /** The per-star key prefix, ':'-terminated. */
    public static String keyPrefix(ByteString schemaChecksum, String rolapStarFactTableName) {
        return schemaChecksum.toString() + ':' + rolapStarFactTableName + ':';
    }

    /**
     * The star part of a segment key ({@code checksum:factTable:uid}):
     * checksum up to the first colon, unique id after the last — the fact
     * table name between them may itself contain colons. Empty for a key
     * that does not carry the shape.
     */
    public static Optional<org.eclipse.daanse.olap.spi.SegmentCache.StarKey>
            starKeyOf(String segmentKey) {
        int first = segmentKey.indexOf(':');
        int last = segmentKey.lastIndexOf(':');
        if (first <= 0 || last <= first + 1 || last >= segmentKey.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(new org.eclipse.daanse.olap.spi.SegmentCache.StarKey(
                segmentKey.substring(0, first), segmentKey.substring(first + 1, last)));
    }

    // self-describing frame: magic, format version, flags, payload.
    // GENERATION must fit a signed byte — past 127 the version wraps and old
    // entries would pass as current
    private static final byte[] MAGIC = { 'D', 'S', 'E', 'G' };
    private static final byte VERSION = (byte) org.eclipse.daanse.olap.spi.SegmentWire.GENERATION;
    static {
        // hard guard, not an assert: with -da a wrapped VERSION byte would
        // let old frames pass as current - the exact mis-read the frame
        // exists to prevent
        if (org.eclipse.daanse.olap.spi.SegmentWire.GENERATION > Byte.MAX_VALUE) {
            throw new ExceptionInInitializerError(
                "SegmentWire.GENERATION " + org.eclipse.daanse.olap.spi.SegmentWire.GENERATION
                + " does not fit the frame's version byte");
        }
    }
    private static final int FLAG_DEFLATE = 1;
    /** The payload starts with a 2-byte node-id length + node id (a stamp). */
    private static final int FLAG_STAMPED = 2;
    /**
     * The payload is a pub/sub event: stamp, 1-byte event kind, then a
     * complete inner frame verbatim. Never combined with {@link #FLAG_DEFLATE}
     * — the inner frame already compresses its own payload.
     */
    private static final int FLAG_EVENT = 4;

    // payload type tags: the first payload byte names the encoding
    static final int TYPE_JAVA = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_BODY_INT = 2;
    static final int TYPE_BODY_DOUBLE = 3;
    static final int TYPE_BODY_OBJECT = 4;
    static final int TYPE_BODY_SPARSE = 5;
    private static final int HEADER_LENGTH = MAGIC.length + 2;
    /** Payloads at least this large are stored deflate-compressed. */
    private static final int COMPRESS_THRESHOLD = 512;
    private static final int BUFFER_SIZE = 8 * 1024;

    // bounded pools: executor threads time out and die, and a per-thread
    // compressor's ~256KB native state would only free with the GC.
    // BEST_SPEED — entries are written once and read often.
    private static final int POOL_CAP =
            2 * Runtime.getRuntime().availableProcessors();
    private static final ConcurrentLinkedQueue<Deflater> DEFLATERS =
            new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Inflater> INFLATERS =
            new ConcurrentLinkedQueue<>();
    private static final AtomicInteger DEFLATER_COUNT =
            new AtomicInteger();
    private static final AtomicInteger INFLATER_COUNT =
            new AtomicInteger();

    private static Deflater borrowDeflater() {
        final Deflater pooled = DEFLATERS.poll();
        if (pooled != null) {
            DEFLATER_COUNT.decrementAndGet();
            return pooled;
        }
        return new Deflater(Deflater.BEST_SPEED);
    }

    private static void giveBack(Deflater deflater) {
        deflater.reset();
        if (DEFLATER_COUNT.incrementAndGet() <= POOL_CAP) {
            DEFLATERS.offer(deflater);
        } else {
            DEFLATER_COUNT.decrementAndGet();
            deflater.end();
        }
    }

    private static Inflater borrowInflater() {
        final Inflater pooled = INFLATERS.poll();
        if (pooled != null) {
            INFLATER_COUNT.decrementAndGet();
            return pooled;
        }
        return new Inflater();
    }

    private static void giveBack(Inflater inflater) {
        inflater.reset();
        if (INFLATER_COUNT.incrementAndGet() <= POOL_CAP) {
            INFLATERS.offer(inflater);
        } else {
            INFLATER_COUNT.decrementAndGet();
            inflater.end();
        }
    }

    private static final class FrameBuffer extends ByteArrayOutputStream {
        FrameBuffer(int size) {
            super(size);
        }

        void writeFrameHeader(int flags) {
            write(MAGIC, 0, MAGIC.length);
            write(VERSION);
            write(flags);
        }

        /** The frame bytes; hands out the backing array when it fits exactly. */
        byte[] finished() {
            return count == buf.length ? buf : toByteArray();
        }
    }

    /**
     * Streams one serialization into a frame. Payloads stay in a probe buffer
     * up to the compression threshold; past it the frame commits to deflate
     * and the rest streams through the compressor — a single serialization
     * pass, no plain copy of the full payload.
     */
    private static final class SwitchingFrameOutput extends java.io.OutputStream {
        private final FrameBuffer frame = new FrameBuffer(BUFFER_SIZE);
        private final byte[] probe = new byte[COMPRESS_THRESHOLD];
        private final int baseFlags;
        private int probeLength;
        private java.io.OutputStream sink;
        private Deflater deflater;

        SwitchingFrameOutput(int baseFlags) {
            this.baseFlags = baseFlags;
        }

        private void overflow() throws IOException {
            frame.writeFrameHeader(baseFlags | FLAG_DEFLATE);
            deflater = borrowDeflater();
            sink = new DeflaterOutputStream(frame, deflater, BUFFER_SIZE);
            sink.write(probe, 0, probeLength);
        }

        @Override
        public void write(int b) throws IOException {
            if (sink == null) {
                if (probeLength < probe.length) {
                    probe[probeLength++] = (byte) b;
                    return;
                }
                overflow();
            }
            sink.write(b);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            if (sink == null) {
                if (length <= probe.length - probeLength) {
                    System.arraycopy(buffer, offset, probe, probeLength, length);
                    probeLength += length;
                    return;
                }
                overflow();
            }
            sink.write(buffer, offset, length);
        }

        byte[] finish() throws IOException {
            if (sink == null) {
                frame.writeFrameHeader(baseFlags);
                frame.write(probe, 0, probeLength);
            } else {
                sink.close();
            }
            return frame.finished();
        }

        void release() {
            if (deflater != null) {
                giveBack(deflater);
                deflater = null;
            }
        }
    }

    public static byte[] write(Serializable value) {
        try {
            return writeFrame(0, null, value, false);
        } catch (SegmentPayloadCodec.UnknownWireTypeException e) {
            return writeFrame(0, null, value, true);
        }
    }

    /**
     * One serialization pass into a frame: optional stamp, then the payload
     * type tag and the tagged encoding — or the Java-serialized form for
     * values outside the wire universe ({@code forceJava}, and the caller
     * retries with it when the tagged encoder meets an unknown value type).
     */
    private static byte[] writeFrame(int baseFlags, String nodeId, Serializable value, boolean forceJava) {
        SwitchingFrameOutput out = new SwitchingFrameOutput(baseFlags);
        try {
            if (nodeId != null) {
                byte[] node = nodeId.getBytes(StandardCharsets.UTF_8);
                if (node.length > 0xFFFF) {
                    throw new IllegalArgumentException("node id too long");
                }
                out.write(node.length >>> 8);
                out.write(node.length);
                out.write(node, 0, node.length);
            }
            int tag = forceJava ? TYPE_JAVA : typeTagFor(value);
            if (tag == TYPE_JAVA) {
                out.write(TYPE_JAVA);
                try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
                    oos.writeObject(value);
                }
            } else {
                DataOutputStream data = new DataOutputStream(out);
                data.writeByte(tag);
                if (tag == TYPE_HEADER) {
                    SegmentPayloadCodec.writeHeader((SegmentHeader) value, data);
                } else {
                    SegmentPayloadCodec.writeBody((SegmentBody) value, data);
                }
                data.flush();
            }
            return out.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("Segment serialization failed", e);
        } finally {
            out.release();
        }
    }

    private static int typeTagFor(Serializable value) {
        if (value instanceof SegmentHeader) {
            return TYPE_HEADER;
        }
        if (value instanceof org.eclipse.daanse.olap.spi.body.DenseIntSegmentBody) {
            return TYPE_BODY_INT;
        }
        if (value instanceof org.eclipse.daanse.olap.spi.body.DenseDoubleSegmentBody) {
            return TYPE_BODY_DOUBLE;
        }
        if (value instanceof org.eclipse.daanse.olap.spi.body.DenseObjectSegmentBody) {
            return TYPE_BODY_OBJECT;
        }
        if (value instanceof org.eclipse.daanse.olap.spi.body.SparseSegmentBody) {
            return TYPE_BODY_SPARSE;
        }
        return TYPE_JAVA;
    }

    /**
     * Writes a stamped frame: the origin node id sits in the frame payload
     * before the value, so distributed-map entries carry their stamp without
     * a nested envelope; deflate covers stamp and value.
     */
    public static byte[] writeStamped(String nodeId, Serializable value) {
        try {
            return writeFrame(FLAG_STAMPED, nodeId, value, false);
        } catch (SegmentPayloadCodec.UnknownWireTypeException e) {
            return writeFrame(FLAG_STAMPED, nodeId, value, true);
        }
    }

    public static SegmentHeader readHeader(byte[] bytes) {
        return (SegmentHeader) read(bytes);
    }

    public static SegmentBody readBody(byte[] bytes) {
        return (SegmentBody) read(bytes);
    }

    /** Payload dispatch on the type tag; the JAVA form keeps the input filter. */
    private static Object readTagged(InputStream payload) throws IOException {
        int tag = payload.read();
        if (tag < 0) {
            throw new IllegalStateException("Truncated segment payload");
        }
        if (tag == TYPE_JAVA) {
            return readObjectFrom(payload);
        }
        DataInputStream data = new DataInputStream(payload);
        if (tag == TYPE_HEADER) {
            return SegmentPayloadCodec.readHeader(data);
        }
        return SegmentPayloadCodec.readBody(tag, data);
    }

    /** Reads a stamped entry written by {@link #writeStamped}. */
    public static StampedValue readStampedValue(byte[] bytes) {
        checkFrame(bytes);
        int flags = bytes[MAGIC.length + 1] & 0xFF;
        if ((flags & FLAG_STAMPED) == 0) {
            throw new IllegalStateException("Not a stamped segment wire entry");
        }
        boolean deflated = (flags & FLAG_DEFLATE) != 0;
        Inflater inflater = deflated ? borrowInflater() : null;
        InputStream payload =
                new ByteArrayInputStream(bytes, HEADER_LENGTH, bytes.length - HEADER_LENGTH);
        if (deflated) {
            payload = new InflaterInputStream(payload, inflater, BUFFER_SIZE);
        }
        payload = new BoundedInputStream(payload, MAX_BYTES);
        try {
            int high = payload.read();
            int low = payload.read();
            if (high < 0 || low < 0) {
                throw new IOException("truncated stamp");
            }
            byte[] node = payload.readNBytes((high << 8) | low);
            String nodeId = new String(node, StandardCharsets.UTF_8);
            return new StampedValue(nodeId, readTagged(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Segment deserialization failed", e);
        } finally {
            if (inflater != null) {
                giveBack(inflater);
            }
        }
    }

    /**
     * Writes a pub/sub event frame: stamp, event kind and the already-framed
     * header verbatim — no serialization, no second deflate. Layout after the
     * frame header: {@code [2B len][nodeId UTF-8][1B created][header frame]}.
     */
    public static byte[] writeEvent(String nodeId, boolean created, byte[] headerFrame) {
        byte[] node = nodeId.getBytes(StandardCharsets.UTF_8);
        if (node.length > 0xFFFF) {
            throw new IllegalArgumentException("node id too long");
        }
        byte[] out = new byte[HEADER_LENGTH + 2 + node.length + 1 + headerFrame.length];
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
        out[MAGIC.length] = VERSION;
        out[MAGIC.length + 1] = FLAG_EVENT;
        int pos = HEADER_LENGTH;
        out[pos++] = (byte) (node.length >>> 8);
        out[pos++] = (byte) node.length;
        System.arraycopy(node, 0, out, pos, node.length);
        pos += node.length;
        out[pos++] = (byte) (created ? 1 : 0);
        System.arraycopy(headerFrame, 0, out, pos, headerFrame.length);
        return out;
    }

    /**
     * Reads a pub/sub event: a {@link #writeEvent} frame directly, or the
     * wire generation bump).
     */
    public static EventValue readEventValue(byte[] bytes) {
        checkFrame(bytes);
        int flags = bytes[MAGIC.length + 1] & 0xFF;
        if ((flags & FLAG_EVENT) == 0) {
            throw new IllegalStateException("Not an event segment wire entry");
        }
        try {
            int pos = HEADER_LENGTH;
            int length = ((bytes[pos] & 0xFF) << 8) | (bytes[pos + 1] & 0xFF);
            pos += 2;
            String nodeId = new String(bytes, pos, length, StandardCharsets.UTF_8);
            pos += length;
            boolean created = bytes[pos] != 0;
            pos++;
            byte[] header = java.util.Arrays.copyOfRange(bytes, pos, bytes.length);
            return new EventValue(nodeId, created, header);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException("Segment deserialization failed", e);
        }
    }

    /** Upper bound per serialized value; anything larger is treated as corrupt. */
    static final int MAX_BYTES = 64 * 1024 * 1024;

    // the store may be writable by third parties: only Daanse cache types and
    // the JDK value types they contain deserialize, everything else is refused
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "org.eclipse.daanse.**;java.util.**;java.lang.**;java.math.**;!*");

    private static void checkFrame(byte[] bytes) {
        if (bytes.length > MAX_BYTES) {
            throw new IllegalStateException("Serialized segment exceeds " + MAX_BYTES + " bytes");
        }
        if (bytes.length < HEADER_LENGTH
                || bytes[0] != MAGIC[0] || bytes[1] != MAGIC[1]
                || bytes[2] != MAGIC[2] || bytes[3] != MAGIC[3]) {
            throw new IllegalStateException("Not a Daanse segment wire entry");
        }
        if (bytes[MAGIC.length] != VERSION) {
            throw new IllegalStateException(
                    "Unsupported segment wire version " + bytes[MAGIC.length]);
        }
    }

    /**
     * Deserializes from the payload stream, resolving via this bundle's
     * loader (SegmentHeader/SegmentBody live in the olap.spi bundle, not
     * necessarily in the caller's loader); the size bound applies to what
     * deserialization actually consumes.
     */
    private static Object readObjectFrom(InputStream payload) {
        try (ObjectInputStream ois = new ObjectInputStream(payload) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, SegmentCodec.class.getClassLoader());
            }
        }) {
            ois.setObjectInputFilter(FILTER);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Segment deserialization failed", e);
        }
    }

    private static Object read(byte[] bytes) {
        checkFrame(bytes);
        // symmetric to readStampedValue/readEventValue guarding THEIR flag:
        // a stamped/event frame in the plain value channel would dispatch
        // its node-id length bytes as a payload tag and fail with a
        // misleading "deserialization failed" instead of naming the cause.
        // Read-side only - the wire format is unchanged.
        int flags = bytes[MAGIC.length + 1];
        if ((flags & (FLAG_STAMPED | FLAG_EVENT)) != 0) {
            throw new IllegalStateException(
                "stamped/event frame in the plain value channel (flags=" + flags + ")");
        }
        boolean deflated = (flags & FLAG_DEFLATE) != 0;
        Inflater inflater = deflated ? borrowInflater() : null;
        InputStream payload = new ByteArrayInputStream(bytes, HEADER_LENGTH, bytes.length - HEADER_LENGTH);
        if (deflated) {
            payload = new InflaterInputStream(payload, inflater, BUFFER_SIZE);
        }
        payload = new BoundedInputStream(payload, MAX_BYTES);
        try {
            return readTagged(payload);
        } catch (IOException e) {
            throw new IllegalStateException("Segment deserialization failed", e);
        } finally {
            if (inflater != null) {
                giveBack(inflater);
            }
        }
    }

    /** Fails once more than {@code limit} bytes are consumed. */
    private static final class BoundedInputStream extends FilterInputStream {
        private long remaining;

        BoundedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0 && --remaining < 0) {
                throw new IOException("Segment exceeds the decompressed size bound");
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count > 0 && (remaining -= count) < 0) {
                throw new IOException("Segment exceeds the decompressed size bound");
            }
            return count;
        }
    }
}
