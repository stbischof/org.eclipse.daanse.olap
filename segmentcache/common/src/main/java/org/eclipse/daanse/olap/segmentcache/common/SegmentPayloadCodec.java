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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;

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

/**
 * Manual payload encoding for segment headers and bodies (wire type tags
 * {@link SegmentCodec#TYPE_HEADER} …): fixed-width DataOutput fields, tagged
 * values, no Java serialization. A header writes exactly its constructor
 * arguments — the constructor rebuilds identity and hash code. Value sets
 * write their live range only (a subrange {@link ArraySortedSet} view never
 * drags its backing array onto the wire). SQL-null and MDX-null sentinels
 * travel as tags and decode to THEIR instances.
 *
 * Unknown value types throw {@link UnknownWireTypeException}; the caller
 * falls back to the Java-serialized payload form. Malformed input throws
 * {@link IllegalStateException} — stores degrade it to a miss.
 */
final class SegmentPayloadCodec {

    private SegmentPayloadCodec() {
    }

    /** Raised mid-encode on a value outside the wire type universe. */
    static final class UnknownWireTypeException extends RuntimeException {
        UnknownWireTypeException(String message) {
            super(message);
        }
    }

    // value tags
    private static final byte V_NULL = 0;
    private static final byte V_STRING = 1;
    private static final byte V_INTEGER = 2;
    private static final byte V_LONG = 3;
    private static final byte V_DOUBLE = 4;
    private static final byte V_BIG_DECIMAL = 5;
    private static final byte V_BOOLEAN = 6;
    private static final byte V_SQL_NULL = 7;
    private static final byte V_MDX_NULL = 8;
    private static final byte V_SQL_DATE = 9;
    private static final byte V_SQL_TIME = 10;
    private static final byte V_SQL_TIMESTAMP = 11;
    private static final byte V_SHORT = 12;
    private static final byte V_BYTE = 13;
    private static final byte V_FLOAT = 14;
    private static final byte V_BIG_INTEGER = 15;
    private static final byte V_LOCAL_DATE = 16;
    private static final byte V_LOCAL_TIME = 17;
    private static final byte V_LOCAL_DATE_TIME = 18;
    private static final int MIXED = 0xFF;

    // predicate tags
    private static final byte P_VALUE = 1;
    private static final byte P_VALUES = 2;
    private static final byte P_RANGE = 3;
    private static final byte P_AND = 4;
    private static final byte P_OR = 5;
    private static final byte P_NOT = 6;
    private static final byte P_LITERAL = 7;
    private static final byte P_OPAQUE = 8;

    // ------------------------------------------------------------ header

    static void writeHeader(SegmentHeader header, DataOutputStream out) throws IOException {
        writeString(header.schemaName, out);
        writeBytes(header.schemaChecksum.toByteArray(), out);
        writeString(header.cubeName, out);
        writeString(header.measureName, out);
        writeString(header.rolapStarFactTableName, out);
        // the key's canonical wire words - same bytes the BitSet detour wrote
        writeLongs(header.constrainedColsBitKey.toLongArray(), out);
        out.writeInt(header.getConstrainedColumns().size());
        for (SegmentColumn column : header.getConstrainedColumns()) {
            writeColumn(column, out);
        }
        out.writeInt(header.compoundPredicates.size());
        for (SegmentPredicate predicate : header.compoundPredicates) {
            writePredicate(predicate, out);
        }
        out.writeInt(header.getExcludedRegions().size());
        for (SegmentRegion region : header.getExcludedRegions()) {
            out.writeInt(region.columns().size());
            for (SegmentColumn column : region.columns()) {
                writeColumn(column, out);
            }
        }
    }

    static SegmentHeader readHeader(DataInputStream in) throws IOException {
        String schemaName = readString(in);
        ByteString checksum = new ByteString(readBytes(in));
        String cubeName = readString(in);
        String measureName = readString(in);
        String factTableName = readString(in);
        BitKey bitKey = BitKey.Factory.fromLongArray(readLongs(in));
        int columnCount = checkedCount(in.readInt(), 8);
        List<SegmentColumn> columns = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columns.add(readColumn(in));
        }
        int predicateCount = checkedCount(in.readInt(), 8);
        List<SegmentPredicate> predicates = new ArrayList<>(predicateCount);
        for (int i = 0; i < predicateCount; i++) {
            predicates.add(readPredicate(in));
        }
        int regionCount = checkedCount(in.readInt(), 8);
        List<SegmentRegion> regions = new ArrayList<>(regionCount);
        for (int i = 0; i < regionCount; i++) {
            int regionColumns = checkedCount(in.readInt(), 8);
            List<SegmentColumn> region = new ArrayList<>(regionColumns);
            for (int j = 0; j < regionColumns; j++) {
                region.add(readColumn(in));
            }
            regions.add(new SegmentRegion(region));
        }
        return new SegmentHeader(schemaName, checksum, cubeName, measureName,
                columns, predicates, factTableName, bitKey, regions);
    }

    private static void writeColumn(SegmentColumn column, DataOutputStream out) throws IOException {
        writeString(column.columnExpression, out);
        out.writeLong(column.valueCount);
        SortedSet<Comparable> values = column.values;
        out.writeBoolean(values != null);
        if (values != null) {
            writeValueArray(values.toArray(new Comparable[0]), out);
        }
    }

    private static SegmentColumn readColumn(DataInputStream in) throws IOException {
        String expression = readString(in);
        long valueCount = in.readLong();
        SortedSet<Comparable> values = null;
        if (in.readBoolean()) {
            values = new ArraySortedSet<>(readValueArray(in));
        }
        return new SegmentColumn(expression, valueCount, values);
    }

    private static void writePredicate(SegmentPredicate predicate, DataOutputStream out) throws IOException {
        switch (predicate) {
        case SegmentPredicate.Value(String column, Comparable value) -> {
            out.writeByte(P_VALUE);
            writeString(column, out);
            writeValue(value, out);
        }
        case SegmentPredicate.Values(String column, List<Comparable> values) -> {
            out.writeByte(P_VALUES);
            writeString(column, out);
            out.writeInt(values.size());
            for (Comparable value : values) {
                writeValue(value, out);
            }
        }
        case SegmentPredicate.Range(String column, Comparable lower, boolean lowerInclusive,
                Comparable upper, boolean upperInclusive) -> {
            out.writeByte(P_RANGE);
            writeString(column, out);
            writeValue(lower, out);
            out.writeBoolean(lowerInclusive);
            writeValue(upper, out);
            out.writeBoolean(upperInclusive);
        }
        case SegmentPredicate.And(List<SegmentPredicate> children) -> {
            out.writeByte(P_AND);
            writePredicates(children, out);
        }
        case SegmentPredicate.Or(List<SegmentPredicate> children) -> {
            out.writeByte(P_OR);
            writePredicates(children, out);
        }
        case SegmentPredicate.Not(SegmentPredicate child) -> {
            out.writeByte(P_NOT);
            writePredicate(child, out);
        }
        case SegmentPredicate.Literal(boolean value) -> {
            out.writeByte(P_LITERAL);
            out.writeBoolean(value);
        }
        case SegmentPredicate.Opaque(String canonicalForm) -> {
            out.writeByte(P_OPAQUE);
            writeString(canonicalForm, out);
        }
        }
    }

    private static void writePredicates(List<SegmentPredicate> children, DataOutputStream out)
            throws IOException {
        out.writeInt(children.size());
        for (SegmentPredicate child : children) {
            writePredicate(child, out);
        }
    }

    private static SegmentPredicate readPredicate(DataInputStream in) throws IOException {
        byte tag = in.readByte();
        return switch (tag) {
        case P_VALUE -> new SegmentPredicate.Value(readString(in), readComparable(in));
        case P_VALUES -> {
            String column = readString(in);
            int count = checkedCount(in.readInt(), 8);
            List<Comparable> values = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                values.add(readComparable(in));
            }
            yield new SegmentPredicate.Values(column, values);
        }
        case P_RANGE -> new SegmentPredicate.Range(readString(in), readComparable(in), in.readBoolean(),
                readComparable(in), in.readBoolean());
        case P_AND -> new SegmentPredicate.And(readPredicates(in));
        case P_OR -> new SegmentPredicate.Or(readPredicates(in));
        case P_NOT -> new SegmentPredicate.Not(readPredicate(in));
        case P_LITERAL -> new SegmentPredicate.Literal(in.readBoolean());
        case P_OPAQUE -> new SegmentPredicate.Opaque(readString(in));
        default -> throw new IllegalStateException("Unknown predicate tag " + tag);
        };
    }

    private static List<SegmentPredicate> readPredicates(DataInputStream in) throws IOException {
        int count = checkedCount(in.readInt(), 8);
        List<SegmentPredicate> children = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            children.add(readPredicate(in));
        }
        return children;
    }

    // ------------------------------------------------------------ bodies

    static void writeBody(SegmentBody body, DataOutputStream out) throws IOException {
        writeAxes(body, out);
        switch (body) {
        case DenseIntSegmentBody b -> {
            writeBitSet(b.getNullValueIndicators(), out);
            int[] values = (int[]) b.getValueArray();
            out.writeInt(values.length);
            for (int value : values) {
                out.writeInt(value);
            }
        }
        case DenseDoubleSegmentBody b -> {
            writeBitSet(b.getNullValueIndicators(), out);
            double[] values = (double[]) b.getValueArray();
            out.writeInt(values.length);
            for (double value : values) {
                out.writeDouble(value);
            }
        }
        case DenseObjectSegmentBody b -> {
            Object[] values = (Object[]) b.getValueArray();
            writeValueArray(values, out);
        }
        case SparseSegmentBody b -> {
            var valueMap = b.getValueMap();
            out.writeInt(valueMap.size());
            int arity = b.getAxisValueSets().length;
            for (var entry : valueMap.entrySet()) {
                int[] ordinals = entry.getKey().getOrdinals();
                if (ordinals.length != arity) {
                    throw new IllegalStateException("cell key arity mismatch");
                }
                for (int ordinal : ordinals) {
                    out.writeInt(ordinal);
                }
                writeValue(entry.getValue(), out);
            }
        }
        default -> throw new UnknownWireTypeException(body.getClass().getName());
        }
    }

    static SegmentBody readBody(int typeTag, DataInputStream in) throws IOException {
        List<Pair<SortedSet<Comparable>, Boolean>> axes = readAxes(in);
        switch (typeTag) {
        case SegmentCodec.TYPE_BODY_INT: {
            BitSet nulls = readBitSet(in);
            int count = checkedCount(in.readInt(), 4);
            checkDenseCount(count, axes);
            int[] values = new int[count];
            for (int i = 0; i < count; i++) {
                values[i] = in.readInt();
            }
            return new DenseIntSegmentBody(nulls, values, axes);
        }
        case SegmentCodec.TYPE_BODY_DOUBLE: {
            BitSet nulls = readBitSet(in);
            int count = checkedCount(in.readInt(), 8);
            checkDenseCount(count, axes);
            double[] values = new double[count];
            for (int i = 0; i < count; i++) {
                values[i] = in.readDouble();
            }
            return new DenseDoubleSegmentBody(nulls, values, axes);
        }
        case SegmentCodec.TYPE_BODY_OBJECT: {
            Object[] values = readObjectArray(in);
            checkDenseCount(values.length, axes);
            return new DenseObjectSegmentBody(values, axes);
        }
        case SegmentCodec.TYPE_BODY_SPARSE: {
            int count = checkedCount(in.readInt(), 8);
            int arity = axes.size();
            CellKey[] keys = new CellKey[count];
            Object[] data = new Object[count];
            for (int i = 0; i < count; i++) {
                int[] ordinals = new int[arity];
                for (int j = 0; j < arity; j++) {
                    ordinals[j] = checkedOrdinal(in.readInt(), axes.get(j));
                }
                keys[i] = CellKey.Generator.newCellKey(ordinals);
                data[i] = readValue(in);
            }
            return new SparseSegmentBody(keys, data, axes);
        }
        default:
            throw new IllegalStateException("Unknown body tag " + typeTag);
        }
    }

    /**
     * A dense body's value count is exactly the axis grid: the product of
     * each axis's positions (its values, plus one when the axis carries a
     * null coordinate). A manipulated or truncated frame must fail HERE as
     * a decode error - the worker degrades it to a miss - not as an
     * out-of-bounds read deep inside a query.
     */
    private static void checkDenseCount(int count,
            List<Pair<SortedSet<Comparable>, Boolean>> axes) {
        if (count == 0) {
            // the builder's empty-segment convention: no values, full axes
            return;
        }
        long product = 1;
        for (Pair<SortedSet<Comparable>, Boolean> axis : axes) {
            product *= axis.left.size() + (Boolean.TRUE.equals(axis.right) ? 1 : 0);
            if (product > Integer.MAX_VALUE) {
                throw new IllegalStateException("axis product overflows an int");
            }
        }
        if (count != product) {
            throw new IllegalStateException(
                "dense body count " + count + " does not match the axis grid " + product);
        }
    }

    /** A sparse cell ordinal must address a position on its axis. */
    private static int checkedOrdinal(int ordinal,
            Pair<SortedSet<Comparable>, Boolean> axis) {
        int positions = axis.left.size() + (Boolean.TRUE.equals(axis.right) ? 1 : 0);
        if (ordinal < 0 || ordinal >= positions) {
            throw new IllegalStateException(
                "sparse cell ordinal " + ordinal + " outside its axis (" + positions + " positions)");
        }
        return ordinal;
    }

    private static void writeAxes(SegmentBody body, DataOutputStream out) throws IOException {
        SortedSet<Comparable>[] axisValueSets = body.getAxisValueSets();
        boolean[] nullAxisFlags = body.getNullAxisFlags();
        out.writeInt(axisValueSets.length);
        for (int i = 0; i < axisValueSets.length; i++) {
            out.writeBoolean(nullAxisFlags[i]);
            writeValueArray(axisValueSets[i].toArray(new Comparable[0]), out);
        }
    }

    private static List<Pair<SortedSet<Comparable>, Boolean>> readAxes(DataInputStream in)
            throws IOException {
        int axisCount = checkedCount(in.readInt(), 8);
        List<Pair<SortedSet<Comparable>, Boolean>> axes = new ArrayList<>(axisCount);
        for (int i = 0; i < axisCount; i++) {
            boolean nullFlag = in.readBoolean();
            SortedSet<Comparable> values = new ArraySortedSet<>(readValueArray(in));
            axes.add(Pair.of(values, nullFlag));
        }
        return axes;
    }

    // ------------------------------------------------------------ values

    private static byte tagOf(Object value) {
        if (value == null) {
            return V_NULL;
        }
        if (value == Util.sqlNullValue) {
            return V_SQL_NULL;
        }
        if (value instanceof NullValue) {
            return V_MDX_NULL;
        }
        if (value instanceof String) {
            return V_STRING;
        }
        if (value instanceof Integer) {
            return V_INTEGER;
        }
        if (value instanceof Long) {
            return V_LONG;
        }
        if (value instanceof Double) {
            return V_DOUBLE;
        }
        if (value instanceof BigDecimal) {
            return V_BIG_DECIMAL;
        }
        if (value instanceof Boolean) {
            return V_BOOLEAN;
        }
        if (value instanceof java.sql.Timestamp) {
            return V_SQL_TIMESTAMP;
        }
        if (value instanceof java.sql.Date) {
            return V_SQL_DATE;
        }
        if (value instanceof java.sql.Time) {
            return V_SQL_TIME;
        }
        if (value instanceof Short) {
            return V_SHORT;
        }
        if (value instanceof Byte) {
            return V_BYTE;
        }
        if (value instanceof Float) {
            return V_FLOAT;
        }
        if (value instanceof java.math.BigInteger) {
            return V_BIG_INTEGER;
        }
        if (value instanceof java.time.LocalDate) {
            return V_LOCAL_DATE;
        }
        if (value instanceof java.time.LocalTime) {
            return V_LOCAL_TIME;
        }
        if (value instanceof java.time.LocalDateTime) {
            return V_LOCAL_DATE_TIME;
        }
        throw new UnknownWireTypeException(value.getClass().getName());
    }

    private static void writeValue(Object value, DataOutputStream out) throws IOException {
        byte tag = tagOf(value);
        out.writeByte(tag);
        writeValuePayload(tag, value, out);
    }

    private static void writeValuePayload(byte tag, Object value, DataOutputStream out)
            throws IOException {
        switch (tag) {
        case V_STRING -> writeString((String) value, out);
        case V_INTEGER -> out.writeInt((Integer) value);
        case V_LONG -> out.writeLong((Long) value);
        case V_DOUBLE -> out.writeDouble((Double) value);
        case V_BIG_DECIMAL -> writeString(value.toString(), out);
        case V_BOOLEAN -> out.writeBoolean((Boolean) value);
        case V_SQL_DATE -> out.writeLong(((java.sql.Date) value).getTime());
        case V_SQL_TIME -> out.writeLong(((java.sql.Time) value).getTime());
        case V_SQL_TIMESTAMP -> {
            java.sql.Timestamp timestamp = (java.sql.Timestamp) value;
            out.writeLong(timestamp.getTime());
            out.writeInt(timestamp.getNanos());
        }
        case V_SHORT -> out.writeShort((Short) value);
        case V_BYTE -> out.writeByte((Byte) value);
        case V_FLOAT -> out.writeFloat((Float) value);
        case V_BIG_INTEGER -> writeBytes(((java.math.BigInteger) value).toByteArray(), out);
        case V_LOCAL_DATE -> out.writeLong(((java.time.LocalDate) value).toEpochDay());
        case V_LOCAL_TIME -> out.writeLong(((java.time.LocalTime) value).toNanoOfDay());
        case V_LOCAL_DATE_TIME -> {
            java.time.LocalDateTime dateTime = (java.time.LocalDateTime) value;
            out.writeLong(dateTime.toLocalDate().toEpochDay());
            out.writeLong(dateTime.toLocalTime().toNanoOfDay());
        }
        default -> {
            // V_NULL / V_SQL_NULL / V_MDX_NULL carry no payload
        }
        }
    }

    private static Object readValue(DataInputStream in) throws IOException {
        return readValuePayload(in.readByte(), in);
    }

    /** For predicate/axis positions, which are Comparable by contract. */
    private static Comparable readComparable(DataInputStream in) throws IOException {
        return (Comparable) readValue(in);
    }

    private static Object readValuePayload(int tag, DataInputStream in) throws IOException {
        return switch (tag) {
        case V_NULL -> null;
        case V_STRING -> readString(in);
        case V_INTEGER -> in.readInt();
        case V_LONG -> in.readLong();
        case V_DOUBLE -> in.readDouble();
        case V_BIG_DECIMAL -> new BigDecimal(readString(in));
        case V_BOOLEAN -> in.readBoolean();
        case V_SQL_NULL -> Util.sqlNullValue;
        case V_MDX_NULL -> NullValue.INSTANCE;
        case V_SQL_DATE -> new java.sql.Date(in.readLong());
        case V_SQL_TIME -> new java.sql.Time(in.readLong());
        case V_SQL_TIMESTAMP -> {
            java.sql.Timestamp timestamp = new java.sql.Timestamp(in.readLong());
            timestamp.setNanos(in.readInt());
            yield timestamp;
        }
        case V_SHORT -> in.readShort();
        case V_BYTE -> in.readByte();
        case V_FLOAT -> in.readFloat();
        case V_BIG_INTEGER -> new java.math.BigInteger(readBytes(in));
        case V_LOCAL_DATE -> java.time.LocalDate.ofEpochDay(in.readLong());
        case V_LOCAL_TIME -> java.time.LocalTime.ofNanoOfDay(in.readLong());
        case V_LOCAL_DATE_TIME -> java.time.LocalDateTime.of(
                java.time.LocalDate.ofEpochDay(in.readLong()),
                java.time.LocalTime.ofNanoOfDay(in.readLong()));
        default -> throw new IllegalStateException("Unknown value tag " + tag);
        };
    }

    /**
     * Homogeneous arrays of one payload-carrying type write a single element
     * tag; anything else (null slots, sentinels, mixed types) writes each
     * value self-tagged.
     */
    private static void writeValueArray(Object[] values, DataOutputStream out) throws IOException {
        out.writeInt(values.length);
        int elemTag = MIXED;
        if (values.length > 0) {
            byte first = tagOf(values[0]);
            if ((first >= V_STRING && first <= V_BOOLEAN)
                    || (first >= V_SQL_DATE && first <= V_LOCAL_DATE_TIME)) {
                elemTag = first;
                for (int i = 1; i < values.length; i++) {
                    if (tagOf(values[i]) != first) {
                        elemTag = MIXED;
                        break;
                    }
                }
            }
        }
        out.writeByte(elemTag);
        if (elemTag == MIXED) {
            for (Object value : values) {
                writeValue(value, out);
            }
        } else {
            for (Object value : values) {
                writeValuePayload((byte) elemTag, value, out);
            }
        }
    }

    /** Axis/column value sets: elements are Comparable by contract. */
    private static Comparable[] readValueArray(DataInputStream in) throws IOException {
        int count = checkedCount(in.readInt(), 8);
        int elemTag = in.readUnsignedByte();
        Comparable[] values = switch (elemTag) {
        case MIXED -> new Comparable[count];
        case V_STRING -> new String[count];
        case V_INTEGER -> new Integer[count];
        case V_LONG -> new Long[count];
        case V_DOUBLE -> new Double[count];
        case V_BIG_DECIMAL -> new BigDecimal[count];
        case V_BOOLEAN -> new Boolean[count];
        case V_SQL_DATE -> new java.sql.Date[count];
        case V_SQL_TIME -> new java.sql.Time[count];
        case V_SQL_TIMESTAMP -> new java.sql.Timestamp[count];
        case V_SHORT -> new Short[count];
        case V_BYTE -> new Byte[count];
        case V_FLOAT -> new Float[count];
        case V_BIG_INTEGER -> new java.math.BigInteger[count];
        case V_LOCAL_DATE -> new java.time.LocalDate[count];
        case V_LOCAL_TIME -> new java.time.LocalTime[count];
        case V_LOCAL_DATE_TIME -> new java.time.LocalDateTime[count];
        default -> throw new IllegalStateException("Unknown array tag " + elemTag);
        };
        for (int i = 0; i < count; i++) {
            values[i] = (Comparable) (elemTag == MIXED ? readValue(in) : readValuePayload(elemTag, in));
        }
        return values;
    }

    /** Cell values: null slots and both null sentinels are legal here. */
    private static Object[] readObjectArray(DataInputStream in) throws IOException {
        int count = checkedCount(in.readInt(), 8);
        int elemTag = in.readUnsignedByte();
        Object[] values = new Object[count];
        for (int i = 0; i < count; i++) {
            values[i] = elemTag == MIXED ? readValue(in) : readValuePayload(elemTag, in);
        }
        return values;
    }

    // ------------------------------------------------------------ primitives

    private static void writeString(String value, DataOutputStream out) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        return new String(readN(length, in), StandardCharsets.UTF_8);
    }

    private static void writeBytes(byte[] bytes, DataOutputStream out) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static byte[] readBytes(DataInputStream in) throws IOException {
        return readN(in.readInt(), in);
    }

    private static void writeBitSet(BitSet bits, DataOutputStream out) throws IOException {
        writeLongs(bits.toLongArray(), out);
    }

    private static BitSet readBitSet(DataInputStream in) throws IOException {
        return BitSet.valueOf(readLongs(in));
    }

    private static void writeLongs(long[] words, DataOutputStream out) throws IOException {
        out.writeInt(words.length);
        for (long word : words) {
            out.writeLong(word);
        }
    }

    private static long[] readLongs(DataInputStream in) throws IOException {
        int wordCount = checkedCount(in.readInt(), 8);
        long[] words = new long[wordCount];
        for (int i = 0; i < wordCount; i++) {
            words[i] = in.readLong();
        }
        return words;
    }

    private static byte[] readN(int length, DataInputStream in) throws IOException {
        checkedCount(length);
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new IllegalStateException("Truncated segment payload");
        }
        return bytes;
    }

    private static int checkedCount(int count) {
        return checkedCount(count, 1);
    }

    /**
     * Guards a length field BEFORE the allocation it sizes: a corrupt or
     * hostile entry must reject, not buy hundreds of megabytes from four
     * bytes (the stream bound only kicks in while READING). The element
     * size caps the allocation at MAX_BYTES.
     */
    private static int checkedCount(int count, int bytesPerElement) {
        if (count < 0 || (long) count * bytesPerElement > SegmentCodec.MAX_BYTES) {
            throw new IllegalStateException("Bad segment payload length " + count);
        }
        return count;
    }
}
