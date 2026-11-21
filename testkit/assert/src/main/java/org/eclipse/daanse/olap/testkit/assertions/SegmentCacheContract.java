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
package org.eclipse.daanse.olap.testkit.assertions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.daanse.olap.key.BitKey;
import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentCache;
import org.eclipse.daanse.olap.spi.SegmentColumn;
import org.eclipse.daanse.olap.spi.SegmentHeader;
import org.eclipse.daanse.olap.util.ByteString;
import org.eclipse.daanse.olap.util.Pair;
import org.eclipse.daanse.olap.spi.body.DenseIntSegmentBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Contract every {@link SegmentCache} implementation must satisfy: freshly
 * deserialized headers are served, header listing never throws, removal
 * reports what it removed, listeners receive create/delete events until
 * removed, tearDown is idempotent. Extend and provide the cache via
 * {@link #createCache()}.
 */
public abstract class SegmentCacheContract {

    /** A fresh cache under test; called once per test method. */
    protected abstract SegmentCache createCache();

    private SegmentCache cache;

    private SegmentCache cache() {
        if (cache == null) {
            cache = createCache();
        }
        return cache;
    }

    @AfterEach
    void tearDownCache() {
        if (cache != null) {
            cache.tearDown();
        }
    }

    protected static SegmentHeader header(String measure) {
        return header(measure, "FACT");
    }

    protected static SegmentHeader header(String measure, String factTable) {
        List<SegmentColumn> columns = List.of(
                new SegmentColumn("[fact].[col]", 10, new TreeSet<>(List.of("a", "b"))));
        return new SegmentHeader("Schema", new ByteString("checksum".getBytes()), "Cube", measure, columns,
                List.of(), factTable, BitKey.Factory.makeBitKey(3), List.of());
    }

    protected static SegmentBody body() {
        return body(new int[] { 1, 2 }, List.of("a", "b"));
    }

    protected static SegmentBody body(int[] values, List<? extends Comparable> axis) {
        List<Pair<TreeSet<Comparable>, Boolean>> axes = new ArrayList<>();
        axes.add(Pair.of(new TreeSet<>(axis), Boolean.FALSE));
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<Pair<java.util.SortedSet<Comparable>, Boolean>> cast = (List) axes;
        return new DenseIntSegmentBody(new BitSet(), values, cast);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(value);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    @Test
    void putThenGetServesAFreshlyDeserializedHeader() throws Exception {
        SegmentHeader header = header("m1");
        assertTrue(cache().put(header, body()));

        SegmentHeader wireHeader = roundTrip(header);
        assertEquals(header, wireHeader);
        assertNotNull(cache().get(wireHeader));
    }

    @Test
    void headerAndBodySurviveSerialization() throws Exception {
        SegmentHeader header = header("m1");
        SegmentBody segmentBody = body();
        assertEquals(header.getUniqueID(), roundTrip(header).getUniqueID());
        assertNotNull(roundTrip(segmentBody));
    }

    @Test
    void getSegmentHeadersNeverThrowsAndListsEntries() {
        assertDoesNotThrow(() -> cache().getSegmentHeaders());
        SegmentHeader header = header("m1");
        cache().put(header, body());
        List<SegmentHeader> headers = assertDoesNotThrow(() -> cache().getSegmentHeaders());
        assertTrue(headers.contains(header));
    }

    @Test
    void removeReportsWhatItRemoved() {
        SegmentHeader header = header("m1");
        assertFalse(cache().remove(header));
        cache().put(header, body());
        assertTrue(cache().remove(header));
        assertFalse(cache().remove(header));
        assertNull(cache().get(header));
    }

    @Test
    void listenersReceiveCreateAndDeleteUntilRemoved() {
        List<SegmentCache.SegmentCacheListener.SegmentCacheEvent> events = new ArrayList<>();
        SegmentCache.SegmentCacheListener listener = events::add;
        cache().addListener(listener);

        SegmentHeader header = header("m1");
        cache().put(header, body());
        cache().remove(header);

        assertTrue(events.stream().anyMatch(
                e -> e.getEventType() == SegmentCache.SegmentCacheListener.SegmentCacheEvent.EventType.ENTRY_CREATED
                        && e.getSource().equals(header)));
        assertTrue(events.stream().anyMatch(
                e -> e.getEventType() == SegmentCache.SegmentCacheListener.SegmentCacheEvent.EventType.ENTRY_DELETED
                        && e.getSource().equals(header)));

        cache().removeListener(listener);
        int delivered = events.size();
        cache().put(header, body());
        assertEquals(delivered, events.size());
    }

    @Test
    void overwritingAHeaderServesTheLatestBody() {
        SegmentHeader header = header("m1");
        assertTrue(cache().put(header, body(new int[] { 1, 2 }, List.of("a", "b"))));
        assertTrue(cache().put(header, body(new int[] { 7, 8 }, List.of("a", "b"))));

        SegmentBody served = cache().get(header);
        assertNotNull(served);
        assertTrue(served.getValueMap().containsValue(7));
    }

    @Test
    void largeBodiesRoundTrip() {
        List<Integer> axis = new ArrayList<>();
        int[] values = new int[100_000];
        for (int i = 0; i < values.length; i++) {
            axis.add(i);
            values[i] = i;
        }
        SegmentHeader header = header("m1");
        assertTrue(cache().put(header, body(values, axis)));
        assertNotNull(cache().get(header));
    }

    @Test
    void concurrentPutsAndGetsAreSafe() throws Exception {
        int threads = 4;
        int perThread = 25;
        SegmentCache cache = cache();
        List<Thread> workers = new ArrayList<>();
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int t = 0; t < threads; t++) {
            final int id = t;
            Thread worker = new Thread(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        SegmentHeader header = header("m" + id + "x" + i);
                        cache.put(header, body());
                        cache.get(header);
                    }
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join(60_000);
        }
        assertTrue(failures.isEmpty(), () -> "concurrent access failed: " + failures.get(0));
        assertNotNull(cache().get(header("m0x0")));
    }

    @Test
    void renameServesTheBodyUnderTheNewHeaderOnly() {
        SegmentHeader oldHeader = header("m1");
        SegmentHeader newHeader = header("m2");
        assertFalse(cache().rename(oldHeader, newHeader));

        cache().put(oldHeader, body());
        assertTrue(cache().rename(oldHeader, newHeader));
        assertNull(cache().get(oldHeader));
        assertNotNull(cache().get(newHeader));
        assertFalse(cache().getSegmentHeaders().contains(oldHeader));
    }

    @Test
    void filteredListingReturnsOnlyTheStarsHeaders() {
        // '_' in the fact table name must not act as a LIKE wildcard
        SegmentHeader mine = header("m1", "FACT_A");
        SegmentHeader wildcardTrap = header("m1", "FACTxA");
        SegmentHeader other = header("m1", "OTHER");
        cache().put(mine, body());
        cache().put(wildcardTrap, body());
        cache().put(other, body());

        List<SegmentHeader> filtered = cache().getSegmentHeaders(
                mine.schemaChecksum, "FACT_A");
        assertTrue(filtered.contains(mine));
        assertFalse(filtered.contains(wildcardTrap));
        assertFalse(filtered.contains(other));
    }

    /** tearDown closes the instance; a shared backing store keeps its entries. */
    @Test
    void tearDownIsIdempotentAndStopsServing() {
        SegmentHeader header = header("m1");
        cache().put(header, body());
        cache().tearDown();
        assertNull(cache().get(header));
        assertFalse(cache().put(header, body()));
        assertDoesNotThrow(() -> cache().tearDown());
        assertDoesNotThrow(() -> cache().getSegmentHeaders());
    }

    @Test
    void renameMovesTheBodyToTheNewHeader() throws Exception {
        SegmentHeader oldHeader = header("m-rename-old");
        SegmentHeader newHeader = header("m-rename-new");
        assertTrue(cache().put(oldHeader, body()));

        assertTrue(cache().rename(oldHeader, newHeader),
            "rename must report success");
        SegmentBody moved = cache().get(roundTrip(newHeader));
        assertNotNull(moved, "the body must be served under the new header");
        assertNull(cache().get(oldHeader),
            "the old header must no longer serve the body");
    }

    @Test
    void renameOfAMissingHeaderDoesNotThrow() {
        assertDoesNotThrow(() ->
            cache().rename(header("m-rename-missing"), header("m-rename-target")));
    }

    @Test
    void perStarListingFiltersByChecksumAndFactTable() {
        SegmentHeader factA = header("m-star-a", "FACT_A");
        SegmentHeader factB = header("m-star-b", "FACT_B");
        assertTrue(cache().put(factA, body()));
        assertTrue(cache().put(factB, body()));

        List<SegmentHeader> onlyA = cache().getSegmentHeaders(
            factA.schemaChecksum, "FACT_A");
        assertTrue(onlyA.contains(factA),
            "the per-star listing must contain the star's header");
        assertFalse(onlyA.contains(factB),
            "the per-star listing must not leak another star's header");

        assertTrue(cache().getSegmentHeaders(
            new ByteString("other-checksum".getBytes()), "FACT_A").isEmpty(),
            "a foreign checksum matches nothing");
        assertTrue(cache().getSegmentHeaders(
            factA.schemaChecksum, "NO_SUCH_FACT").isEmpty(),
            "an unknown fact table matches nothing");
    }

    @Test
    void knownStarsListsEveryStoredStarWithoutTheInventory() {
        assertTrue(cache().knownStars().isEmpty(),
            "a fresh cache knows no stars");
        SegmentHeader factA = header("m-stars-a", "FACT_A");
        SegmentHeader factB = header("m-stars-b", "FACT_B");
        assertTrue(cache().put(factA, body()));
        assertTrue(cache().put(factB, body()));

        java.util.Set<SegmentCache.StarKey> stars = cache().knownStars();
        assertTrue(stars.contains(SegmentCache.StarKey.of(factA)),
            "knownStars must list FACT_A's star");
        assertTrue(stars.contains(SegmentCache.StarKey.of(factB)),
            "knownStars must list FACT_B's star");

        assertTrue(cache().remove(factA));
        assertTrue(cache().remove(factB));
    }
}
