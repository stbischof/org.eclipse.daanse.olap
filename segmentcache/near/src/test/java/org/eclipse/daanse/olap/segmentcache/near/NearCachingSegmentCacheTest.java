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
package org.eclipse.daanse.olap.segmentcache.near;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentCache;
import org.eclipse.daanse.olap.spi.SegmentCache.SegmentCacheListener.SegmentCacheEvent.EventType;
import org.eclipse.daanse.olap.spi.SegmentHeader;
import org.eclipse.daanse.olap.segmentcache.common.SegmentCodec;
import org.eclipse.daanse.olap.testkit.assertions.SegmentCacheContract;
import org.junit.jupiter.api.Test;

class NearCachingSegmentCacheTest extends SegmentCacheContract {

    private final MapSegmentCache delegate = new MapSegmentCache();
    private final NearCachingSegmentCache near = new NearCachingSegmentCache(delegate, 1_000_000);

    @Override
    protected SegmentCache createCache() {
        return near;
    }

    @Test
    void repeatedGetsHitTheNearLayerNotTheDelegate() {
        SegmentHeader header = header("m1");
        near.put(header, body());

        near.get(header);
        near.get(header);
        near.get(header);

        assertThat(delegate.gets).hasValue(0);
    }

    @Test
    void remoteHitIsStoredLocally() {
        SegmentHeader header = header("m1");
        delegate.bodies.put(SegmentCodec.key(header).value(), body());

        assertThat(near.get(header)).isNotNull();
        assertThat(near.get(header)).isNotNull();

        assertThat(delegate.gets).hasValue(1);
    }

    @Test
    void foreignDeleteEventEvictsTheNearEntry() {
        SegmentHeader header = header("m1");
        near.put(header, body());
        delegate.bodies.remove(SegmentCodec.key(header).value());

        delegate.fireForeign(header, EventType.ENTRY_DELETED);

        assertThat(near.get(header)).isNull();
    }

    /**
     * A foreign CREATE overwrites the body under an existing id (rename
     * target, re-publish): a stale near entry would shadow the fresh
     * delegate body until expiry. Red while the invalidator ignored
     * ENTRY_CREATED.
     */
    @Test
    void foreignCreateEvictsTheStaleNearEntry() {
        SegmentHeader header = header("m1");
        near.put(header, body());
        // a foreign publication lands under the same id (the body object
        // itself is irrelevant - the eviction is what is under test)
        delegate.bodies.put(SegmentCodec.key(header).value(),
            delegate.bodies.values().iterator().next());

        delegate.fireForeign(header, EventType.ENTRY_CREATED);

        // the next read must go to the delegate, not the stale near copy
        int getsBefore = delegate.gets.get();
        near.get(header);
        assertThat(delegate.gets.get()).isGreaterThan(getsBefore);
    }

    /**
     * A rename onto a target id the near layer already holds (rename
     * chains, collision targets) must not shadow the post-rename body.
     * Red while rename only invalidated the OLD header.
     */
    @Test
    void renameEvictsTheTargetIdToo() {
        SegmentHeader oldHeader = header("m-old");
        SegmentHeader newHeader = header("m-new");
        near.put(oldHeader, body());
        near.put(newHeader, body()); // stale target-side entry

        near.rename(oldHeader, newHeader);

        int getsBefore = delegate.gets.get();
        near.get(newHeader);
        assertThat(delegate.gets.get())
            .as("the post-rename body must come from the delegate")
            .isGreaterThan(getsBefore);
    }

    @Test
    void dsConstructorDecoratesTheDelegateAndTearDownKeepsItsStore() {
        MapSegmentCache backing = new MapSegmentCache();
        NearCachingSegmentCache layered = new NearCachingSegmentCache(backing, config(1_000_000));
        SegmentHeader header = header("m1");

        layered.put(header, body());
        layered.get(header);
        layered.get(header);

        assertThat(backing.gets).hasValue(0); // served by the near layer
        layered.tearDown();
        assertThat(backing.bodies).isNotEmpty(); // delegate keeps its store
    }

    private static NearCachingSegmentCache.Config config(long maxWeightCells) {
        return new NearCachingSegmentCache.Config() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return NearCachingSegmentCache.Config.class;
            }

            @Override
            public long maxWeightCells() {
                return maxWeightCells;
            }

            @Override
            public long expireSeconds() {
                return 600;
            }
        };
    }

    @Test
    void refusedDelegatePutIsNotServedLocally() {
        MapSegmentCache refusing = new MapSegmentCache() {
            @Override
            public boolean put(SegmentHeader header, SegmentBody body) {
                return false;
            }
        };
        NearCachingSegmentCache layered = new NearCachingSegmentCache(refusing, 1_000_000);
        SegmentHeader header = header("m1");

        assertThat(layered.put(header, body())).isFalse();
        assertThat(layered.get(header)).isNull();
        layered.tearDown();
    }

    /**
     * The cross-node twin of the racing-get test: a FOREIGN delete event
     * has only its single invalidation, so a get that read the delegate
     * just before the event re-checks the epoch before installing what it
     * read.
     */
    @Test
    void aForeignDeleteDuringTheFillWindowIsNotOutlived() throws Exception {
        CountDownLatch inWindow = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MapSegmentCache backing = new MapSegmentCache() {
            @Override
            public SegmentBody get(SegmentHeader header) {
                SegmentBody body = super.get(header);
                inWindow.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return body;
            }
        };
        NearCachingSegmentCache layered = new NearCachingSegmentCache(backing, 1_000_000);
        SegmentHeader h = header("foreign-race");
        backing.bodies.put(SegmentCodec.key(h).value(), body()); // remote data, near empty

        SegmentBody[] read = new SegmentBody[1];
        Thread reader = new Thread(() -> read[0] = layered.get(h));
        reader.start();
        assertThat(inWindow.await(5, TimeUnit.SECONDS)).isTrue();
        // the foreign flush lands while the reader waits on the delegate
        backing.bodies.remove(SegmentCodec.key(h).value());
        backing.fireForeign(h, EventType.ENTRY_DELETED);
        release.countDown();
        reader.join(5_000);

        assertThat(read[0]).as("the reader itself may see the pre-flush body").isNotNull();
        assertThat(layered.get(h))
            .as("but the near cache must not serve it after the foreign delete")
            .isNull();
        layered.tearDown();
    }

    /**
     * The LOCAL twin of the foreign fill-window race: a reader samples the
     * epoch and reads the delegate, then a local remove() runs COMPLETELY
     * (both invalidations included) before the reader's near.put lands.
     * Foreign events bump the epoch, local delegate events deliberately
     * do not, so remove() bumps it itself after the delegate call - the
     * reader's self-check then fails and the flushed body is not
     * installed.
     */
    @Test
    void aLocalRemoveCompletingInsideTheFillWindowIsNotOutlived() throws Exception {
        CountDownLatch inWindow = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MapSegmentCache backing = new MapSegmentCache() {
            @Override
            public SegmentBody get(SegmentHeader header) {
                SegmentBody body = super.get(header);
                if (body != null) {
                    inWindow.countDown();
                    try {
                        release.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return body;
            }
        };
        NearCachingSegmentCache layered = new NearCachingSegmentCache(backing, 1_000_000);
        SegmentHeader h = header("local-race");
        backing.bodies.put(SegmentCodec.key(h).value(), body()); // remote data, near empty

        SegmentBody[] read = new SegmentBody[1];
        Thread reader = new Thread(() -> read[0] = layered.get(h));
        reader.start();
        assertThat(inWindow.await(5, TimeUnit.SECONDS)).isTrue();
        // the LOCAL flush runs to completion while the reader still holds
        // the pre-flush body it read from the delegate
        layered.remove(h);
        release.countDown();
        reader.join(5_000);

        assertThat(read[0]).as("the reader itself may see the pre-flush body").isNotNull();
        assertThat(layered.get(h))
            .as("but the near cache must not serve it after the local remove")
            .isNull();
        layered.tearDown();
    }

    /**
     * A get racing into the window between the leading invalidation and
     * the delegate's remove re-populates the near cache from the still-
     * present backing body, so the trailing invalidation removes what that
     * get installed.
     */
    @Test
    void removeSurvivesARacingGetRepopulation() throws Exception {
        CountDownLatch removing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MapSegmentCache backing = new MapSegmentCache() {
            @Override
            public boolean remove(SegmentHeader h) {
                removing.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.remove(h);
            }
        };
        NearCachingSegmentCache layered = new NearCachingSegmentCache(backing, 1_000_000);
        SegmentHeader h = header("race");
        layered.put(h, body());

        Thread remover = new Thread(() -> layered.remove(h));
        remover.start();
        assertThat(removing.await(5, TimeUnit.SECONDS)).isTrue();
        // in the window: near was invalidated once, backing still has the
        // body - this get re-populates the near cache
        assertThat(layered.get(h)).isNotNull();
        release.countDown();
        remover.join(5_000);

        assertThat(layered.get(h))
            .as("the flushed body must not survive in the near cache")
            .isNull();
    }
}
