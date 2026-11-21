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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.daanse.olap.api.result.NullValue;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.key.BitKey;
import org.eclipse.daanse.olap.key.CellKey;
import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentColumn;
import org.eclipse.daanse.olap.spi.SegmentHeader;
import org.eclipse.daanse.olap.spi.SegmentPredicate;
import org.eclipse.daanse.olap.spi.SegmentRegion;
import org.eclipse.daanse.olap.spi.body.DenseDoubleSegmentBody;
import org.eclipse.daanse.olap.spi.body.DenseIntSegmentBody;
import org.eclipse.daanse.olap.spi.body.DenseObjectSegmentBody;
import org.eclipse.daanse.olap.spi.body.SparseSegmentBody;
import org.eclipse.daanse.olap.util.ArraySortedSet;
import org.eclipse.daanse.olap.util.ByteString;
import org.eclipse.daanse.olap.util.Pair;
import org.junit.jupiter.api.Test;

/** Tagged wire encoding: round trips, sentinel identity, size behavior. */
@SuppressWarnings({ "unchecked", "rawtypes" })
class SegmentPayloadCodecTest {

    private static SegmentHeader headerFixture(BitKey bitKey) {
        List<SegmentPredicate> predicates = List.of(
                new SegmentPredicate.And(List.of(
                        new SegmentPredicate.Value("[f].[a]", "x"),
                        new SegmentPredicate.Or(List.of(
                                new SegmentPredicate.Values("[f].[b]", List.of(1, 2, 3)),
                                new SegmentPredicate.Not(new SegmentPredicate.Literal(false)))))),
                new SegmentPredicate.Range("[f].[c]", null, false, 9.5, true),
                new SegmentPredicate.Opaque("opaque canonical text"));
        List<SegmentColumn> columns = List.of(
                new SegmentColumn("[f].[str]", 10, new ArraySortedSet(new Comparable[] { "a", "b" })),
                new SegmentColumn("[f].[int]", 20, new ArraySortedSet(new Comparable[] { 1, 2, 3 })),
                new SegmentColumn("[f].[dec]", 5,
                        new ArraySortedSet(new Comparable[] { new BigDecimal("1.50") })),
                new SegmentColumn("[f].[mixed]", 4, mixedSet()),
                new SegmentColumn("[f].[wild]", 7, null));
        List<SegmentRegion> regions = List.of(new SegmentRegion(List.of(
                new SegmentColumn("[f].[str]", 10, new ArraySortedSet(new Comparable[] { "a" })))));
        return new SegmentHeader("Schema", new ByteString("checksum".getBytes()), "Cube",
                "measure", columns, predicates, "FACT", bitKey, regions);
    }

    /** Sentinel-first ordering, matching how header columns sort SQL nulls. */
    private static final class SentinelFirst
            implements java.util.Comparator<Object>, java.io.Serializable {
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public int compare(Object a, Object b) {
            if (a == b) {
                return 0;
            }
            if (a == Util.sqlNullValue) {
                return -1;
            }
            if (b == Util.sqlNullValue) {
                return 1;
            }
            return ((Comparable) a).compareTo(b);
        }
    }

    private static SortedSet<Comparable> mixedSet() {
        TreeSet<Comparable> set = new TreeSet<>(new SentinelFirst());
        set.add(7L);
        set.add((Comparable) Util.sqlNullValue);
        return set;
    }

    private static List<Pair<SortedSet<Comparable>, Boolean>> axes() {
        return List.of(
                Pair.of((SortedSet<Comparable>) new ArraySortedSet(new Comparable[] { "a", "b" }), Boolean.FALSE),
                Pair.of((SortedSet<Comparable>) new ArraySortedSet(new Comparable[] { 1, 2 }), Boolean.TRUE));
    }

    @Test
    void headerRoundTripsWithFullFixture() {
        for (BitKey bitKey : List.of(smallKey(), bigKey())) {
            SegmentHeader header = headerFixture(bitKey);
            SegmentHeader read = SegmentCodec.readHeader(SegmentCodec.write(header));
            assertThat(read).isEqualTo(header);
            assertThat(read.getUniqueID()).isEqualTo(header.getUniqueID());
            assertThat((Object) read.constrainedColsBitKey).isEqualTo(bitKey);
            assertThat(read.compoundPredicates).isEqualTo(header.compoundPredicates);
            assertThat(read.getExcludedRegions()).isEqualTo(header.getExcludedRegions());
        }
    }

    @Test
    void sentinelsKeepInstanceIdentity() {
        SegmentHeader read = SegmentCodec.readHeader(SegmentCodec.write(headerFixture(smallKey())));
        SortedSet<Comparable> mixed = read.getConstrainedColumns().get(3).values;
        assertThat(mixed.stream().anyMatch(v -> v == Util.sqlNullValue)).isTrue();

        SegmentBody body = new DenseObjectSegmentBody(
                new Object[] { "s", null, Util.sqlNullValue, NullValue.INSTANCE, new BigDecimal("2.25"), 7L },
                axes());
        Object[] values = (Object[]) SegmentCodec.readBody(SegmentCodec.write(body)).getValueArray();
        assertThat(values[1]).isNull();
        assertThat(values[2]).isSameAs(Util.sqlNullValue);
        assertThat(values[3]).isSameAs(NullValue.INSTANCE);
        assertThat(values[4]).isEqualTo(new BigDecimal("2.25"));
    }

    @Test
    void allBodyShapesRoundTrip() {
        BitSet nulls = new BitSet();
        nulls.set(1);
        // the value count fills the 2x3 axis grid exactly - the decoder
        // rejects any other dense count as a malformed frame
        SegmentBody intBody = new DenseIntSegmentBody(nulls, new int[] { 1, 0, 3, 4, 5, 6 }, axes());
        SegmentBody doubleBody = new DenseDoubleSegmentBody(nulls, new double[] { 1.5, 0, 3.5, 4.5, 5.5, 6.5 }, axes());
        SegmentBody objectBody = new DenseObjectSegmentBody(new Object[] { "a", 2L, null, 4.0, "e", 6L }, axes());
        SegmentBody sparseBody = new SparseSegmentBody(Map.of(
                CellKey.Generator.newCellKey(new int[] { 0, 1 }), (Object) 42.0,
                CellKey.Generator.newCellKey(new int[] { 1, 0 }), (Object) "v"), axes());

        for (SegmentBody body : List.of(intBody, doubleBody, objectBody, sparseBody)) {
            SegmentBody read = SegmentCodec.readBody(SegmentCodec.write(body));
            assertThat(read.getClass()).isEqualTo(body.getClass());
            assertThat(read.getValueMap()).isEqualTo(body.getValueMap());
            assertThat(read.getNullAxisFlags()).isEqualTo(body.getNullAxisFlags());
            assertThat(read.getAxisValueSets()).isEqualTo(body.getAxisValueSets());
        }

        // past the deflate threshold; axis matches the value count
        int[] many = new int[4096];
        Comparable[] axisValues = new Comparable[4096];
        for (int i = 0; i < many.length; i++) {
            many[i] = i;
            axisValues[i] = i;
        }
        SegmentBody large = new DenseIntSegmentBody(new BitSet(), many,
                List.of(Pair.of((SortedSet<Comparable>) new ArraySortedSet(axisValues), Boolean.FALSE)));
        SegmentBody largeRead = SegmentCodec.readBody(SegmentCodec.write(large));
        assertThat((int[]) largeRead.getValueArray()).isEqualTo(many);
        assertThat(largeRead.getAxisValueSets()).isEqualTo(large.getAxisValueSets());
    }

    /**
     * A dense body whose value count does not match the axis grid (2x3
     * here) is a malformed frame and must fail AT DECODE - the worker
     * degrades it to a miss - not as an out-of-bounds read deep inside a
     * query. The empty body (builder convention for empty segments) stays
     * legal.
     */
    @Test
    void denseCountMustMatchTheAxisGrid() {
        byte[] shortInt = SegmentCodec.write(
                new DenseIntSegmentBody(new BitSet(), new int[] { 1, 2 }, axes()));
        byte[] shortDouble = SegmentCodec.write(
                new DenseDoubleSegmentBody(new BitSet(), new double[] { 1.5 }, axes()));
        byte[] shortObject = SegmentCodec.write(
                new DenseObjectSegmentBody(new Object[] { "a" }, axes()));
        for (byte[] wire : List.of(shortInt, shortDouble, shortObject)) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> SegmentCodec.readBody(wire));
        }

        SegmentBody empty = new DenseObjectSegmentBody(new Object[0], axes());
        assertThat((Object[]) SegmentCodec.readBody(SegmentCodec.write(empty)).getValueArray())
                .isEmpty();
    }

    /**
     * A sparse cell ordinal outside its axis (axis 0 has 2 positions,
     * axis 1 has 2 values plus a null coordinate = 3) is a malformed
     * frame and must fail at decode instead of blowing up cell math later.
     */
    @Test
    void sparseOrdinalsMustAddressTheirAxis() {
        byte[] badAxis0 = SegmentCodec.write(new SparseSegmentBody(Map.of(
                CellKey.Generator.newCellKey(new int[] { 9, 0 }), (Object) 1.0), axes()));
        byte[] nullCoordinateOnAxis1 = SegmentCodec.write(new SparseSegmentBody(Map.of(
                CellKey.Generator.newCellKey(new int[] { 0, 2 }), (Object) 1.0), axes()));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> SegmentCodec.readBody(badAxis0));
        // ordinal 2 on the null-flagged axis IS the null coordinate - legal
        assertThat(SegmentCodec.readBody(nullCoordinateOnAxis1).getValueMap()).hasSize(1);
    }

    @Test
    void subrangeViewsWriteOnlyTheirLiveRange() {
        Comparable[] smallBacking = new Comparable[5];
        Comparable[] hugeBacking = new Comparable[5000];
        for (int i = 0; i < hugeBacking.length; i++) {
            hugeBacking[i] = i;
        }
        System.arraycopy(hugeBacking, 0, smallBacking, 0, 5);
        SegmentHeader small = subrangeHeader(new ArraySortedSet(smallBacking, 1, 4));
        SegmentHeader huge = subrangeHeader(new ArraySortedSet(hugeBacking, 1, 4));
        assertThat(SegmentCodec.write(huge).length).isEqualTo(SegmentCodec.write(small).length);
        assertThat(SegmentCodec.readHeader(SegmentCodec.write(huge))).isEqualTo(huge);
    }

    private static SegmentHeader subrangeHeader(SortedSet<Comparable> values) {
        return new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(new SegmentColumn("[f].[x]", 5, values)), List.of(), "FACT",
                smallKey(), List.of());
    }

    @Test
    void taggedHeaderIsLessThanHalfTheJavaSerializedSize() throws Exception {
        SegmentHeader header = headerFixture(smallKey());
        byte[] codec = SegmentCodec.write(header);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(header);
        }
        System.out.println("wire size: codec=" + codec.length + "B oos=" + baos.size() + "B");
        assertThat(codec.length).isLessThan(baos.size() / 2);
    }

    @Test
    void unknownComparableFallsBackToJavaSerialization() {
        SegmentHeader header = new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(new SegmentColumn("[f].[t]", 1,
                        new ArraySortedSet(new Comparable[] {
                                java.time.Year.of(2026) }))),
                List.of(), "FACT", smallKey(), List.of());
        byte[] wire = SegmentCodec.write(header);
        // java.time.Year has no wire tag and is outside the deserialization
        // allowlist: the fallback form is written, and reading it back is
        // refused deterministically
        assertThatThrownBy(() -> SegmentCodec.readHeader(wire))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sqlDateTimeAxisValuesTravelTagged() {
        java.sql.Timestamp timestamp = new java.sql.Timestamp(1_234_567_890_123L);
        timestamp.setNanos(123456789);
        SegmentHeader header = new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(
                        new SegmentColumn("[f].[d]", 3, new ArraySortedSet(new Comparable[] {
                                new java.sql.Date(0L), new java.sql.Date(86_400_000L) })),
                        new SegmentColumn("[f].[t]", 1, new ArraySortedSet(new Comparable[] {
                                new java.sql.Time(12_000L) })),
                        new SegmentColumn("[f].[ts]", 1, new ArraySortedSet(new Comparable[] {
                                timestamp }))),
                List.of(), "FACT", smallKey(), List.of());
        byte[] wire = SegmentCodec.write(header);
        // tagged header form, not the Java fallback
        assertThat(wire[6]).isEqualTo((byte) SegmentCodec.TYPE_HEADER);
        SegmentHeader read = SegmentCodec.readHeader(wire);
        assertThat(read).isEqualTo(header);
        SortedSet<Comparable> stamps = read.getConstrainedColumns().get(2).values;
        assertThat(((java.sql.Timestamp) stamps.first()).getNanos()).isEqualTo(123456789);
    }

    @Test
    void sqlDateBodiesRoundTripIncludingDeflate() {
        // homogeneous java.sql.Date axis, java.sql.Timestamp cells, past the
        // deflate threshold
        int n = 2048;
        Comparable[] axisValues = new Comparable[n];
        Object[] cells = new Object[n];
        for (int i = 0; i < n; i++) {
            axisValues[i] = new java.sql.Date(i * 86_400_000L);
            cells[i] = new java.sql.Timestamp(i);
        }
        SegmentBody body = new DenseObjectSegmentBody(cells, List.of(
                Pair.of((SortedSet<Comparable>) new ArraySortedSet(axisValues), Boolean.FALSE)));
        SegmentBody read = SegmentCodec.readBody(SegmentCodec.write(body));
        assertThat((Object[]) read.getValueArray()).isEqualTo(cells);
        assertThat(read.getAxisValueSets()).isEqualTo(body.getAxisValueSets());
    }

    @Test
    void malformedTaggedPayloadsAreRefused() {
        byte[] wire = SegmentCodec.write(headerFixture(smallKey()));
        byte[] truncated = java.util.Arrays.copyOf(wire, wire.length / 2);
        assertThatThrownBy(() -> SegmentCodec.readHeader(truncated))
                .isInstanceOf(IllegalStateException.class);
    }

    private static BitKey smallKey() {
        BitKey key = BitKey.Factory.makeBitKey(8);
        key.set(1);
        key.set(3);
        return key;
    }

    private static BitKey bigKey() {
        BitKey key = BitKey.Factory.makeBitKey(300);
        key.set(1);
        key.set(250);
        return key;
    }

    /** Guards the v7 header wire bytes against drift (undeflated frames). */
    @Test
    void headerWireBytesArePinned() {
        SegmentHeader small = subrangeHeader(
                new ArraySortedSet(new Comparable[] { "a" }));
        assertThat(hex(SegmentCodec.write(small))).isEqualTo("44534547070001000000015300000001630000000143000000016d000000044641435400000001000000000000000a00000001000000075b665d2e5b785d000000000000000501000000010100000001610000000000000000");
        SegmentHeader wide = new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(new SegmentColumn("[f].[x]", 5,
                        new ArraySortedSet(new Comparable[] { "a" }))),
                List.of(), "FACT", bigKey(), List.of());
        assertThat(hex(SegmentCodec.write(wide))).isEqualTo("44534547070001000000015300000001630000000143000000016d000000044641435400000004000000000000000200000000000000000000000000000000040000000000000000000001000000075b665d2e5b785d000000000000000501000000010100000001610000000000000000");
    }

    /**
     * Mid128 has the only variant-specific trim logic in toLongArray, and
     * the empty key is the zero-word edge - both byte-pinned so encoder
     * drift cannot silently break compatibility within one GENERATION.
     */
    @Test
    void mid128AndEmptyKeyWireBytesArePinned() {
        BitKey mid = BitKey.Factory.makeBitKey(80);
        mid.set(1);
        mid.set(70);
        SegmentHeader midHeader = new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(new SegmentColumn("[f].[x]", 5,
                        new ArraySortedSet(new Comparable[] { "a" }))),
                List.of(), "FACT", mid, List.of());
        assertThat(hex(SegmentCodec.write(midHeader))).isEqualTo("44534547070001000000015300000001630000000143000000016d0000000446414354000000020000000000000002000000000000004000000001000000075b665d2e5b785d000000000000000501000000010100000001610000000000000000");

        BitKey empty = BitKey.Factory.makeBitKey(8);
        SegmentHeader emptyHeader = new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(new SegmentColumn("[f].[x]", 5,
                        new ArraySortedSet(new Comparable[] { "a" }))),
                List.of(), "FACT", empty, List.of());
        assertThat(hex(SegmentCodec.write(emptyHeader))).isEqualTo("44534547070001000000015300000001630000000143000000016d00000004464143540000000000000001000000075b665d2e5b785d000000000000000501000000010100000001610000000000000000");
    }

    /**
     * A length field must reject BEFORE the allocation it sizes: four
     * corrupt bytes must not buy a 512MB array (the stream bound only
     * kicks in while reading).
     */
    @Test
    void hostileWordCountRejectsWithoutAllocating() {
        byte[] wire = SegmentCodec.write(headerFixture(smallKey()));
        // the bitkey word count sits right after "FACT": patch it huge
        int factIndex = indexOf(wire, "FACT".getBytes());
        int countIndex = factIndex + 4;
        wire[countIndex] = 0x03;
        wire[countIndex + 1] = (byte) 0xFF;
        wire[countIndex + 2] = (byte) 0xFF;
        wire[countIndex + 3] = (byte) 0xFF;
        assertThatThrownBy(() -> SegmentCodec.readHeader(wire))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payload length");
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        throw new IllegalStateException("needle not found");
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void narrowNumericAndJavaTimeValuesTravelTagged() {
        java.time.LocalDateTime dateTime =
                java.time.LocalDateTime.of(2026, 9, 5, 13, 37, 42, 123456789);
        SegmentHeader header = new SegmentHeader("S", new ByteString("c".getBytes()), "C", "m",
                List.of(
                        new SegmentColumn("[f].[s]", 2, new ArraySortedSet(new Comparable[] {
                                Short.valueOf((short) -7), Short.valueOf((short) 42) })),
                        new SegmentColumn("[f].[b]", 1, new ArraySortedSet(new Comparable[] {
                                Byte.valueOf((byte) 5) })),
                        new SegmentColumn("[f].[f]", 1, new ArraySortedSet(new Comparable[] {
                                Float.valueOf(1.5f) })),
                        new SegmentColumn("[f].[bi]", 1, new ArraySortedSet(new Comparable[] {
                                new java.math.BigInteger("123456789012345678901234567890") })),
                        new SegmentColumn("[f].[ld]", 1, new ArraySortedSet(new Comparable[] {
                                java.time.LocalDate.of(2026, 9, 5) })),
                        new SegmentColumn("[f].[lt]", 1, new ArraySortedSet(new Comparable[] {
                                java.time.LocalTime.of(13, 37, 42) })),
                        new SegmentColumn("[f].[ldt]", 1, new ArraySortedSet(new Comparable[] {
                                dateTime }))),
                List.of(), "FACT", smallKey(), List.of());
        byte[] wire = SegmentCodec.write(header);
        // tagged header form, not the Java fallback
        assertThat(wire[6]).isEqualTo((byte) SegmentCodec.TYPE_HEADER);
        SegmentHeader read = SegmentCodec.readHeader(wire);
        assertThat(read).isEqualTo(header);
        assertThat(read.getConstrainedColumns().get(6).values.first())
                .isEqualTo(dateTime);
    }
}
