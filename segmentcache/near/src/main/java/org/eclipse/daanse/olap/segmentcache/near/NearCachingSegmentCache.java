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

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentCache;
import org.eclipse.daanse.olap.spi.SegmentCache.StarKey;
import org.eclipse.daanse.olap.spi.SegmentHeader;
import org.eclipse.daanse.olap.util.ByteString;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

/**
 * Bounded Caffeine layer in front of any (typically remote) {@link SegmentCache};
 * keeps network round-trips out of the query hot path. Works the same in front
 * of Redis, Hazelcast or JDBC.
 *
 * Whiteboard wiring: as a DS component it binds a delegate registered with
 * {@code daanse.segmentcache.hidden=true} (the engine skips such services;
 * {@code delegate.target} is overridable) and republishes the decorated
 * cache as a plain SegmentCache. Programmatic composition uses the
 * {@code (SegmentCache, long)} constructor.
 *
 * Coherency:
 * - get: local first, then delegate; remote hits are stored locally.
 * - put: write-through, delegate first — the near layer only serves what the
 *   remote store holds.
 * - EVERY foreign event evicts the local entry - DELETED and CREATED alike
 *   (a foreign put overwrites the body under an existing id), so the near
 *   layer is at most as stale as the event latency; nothing is preloaded.
 * - remove: local and delegate; the delegate's answer counts.
 * - rename: both ids are invalidated on both sides of the delegate call.
 *
 * Weight is a rough cell count, not entry count — bodies are the bulk, and a
 * few huge segments must not pin the heap.
 */
@Component(service = SegmentCache.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = NearCachingSegmentCache.Config.class)
public final class NearCachingSegmentCache implements SegmentCache {

    /** One truth for the config default and the 2-arg constructor. */
    static final long DEFAULT_EXPIRE_SECONDS = 600;

    @ObjectClassDefinition(name = "Daanse Near SegmentCache")
    public @interface Config {
        @AttributeDefinition(description = "Weight bound of the local layer, in cells")
        long maxWeightCells() default 5_000_000;

        @AttributeDefinition(description = "Seconds until a near entry expires; bounds"
            + " staleness when the delegate has no cross-node delete events."
            + " Keep it <= the store's own TTL: a longer near expiry outlives"
            + " the store's truth and serves entries the store already dropped.")
        long expireSeconds() default DEFAULT_EXPIRE_SECONDS;
    }

    private final SegmentCache delegate;
    // keyed on the header itself: equals/hashCode are digest-based and
    // already computed, no ~100-char key string per access
    private final Cache<SegmentHeader, SegmentBody> near;
    private final SegmentCacheListener invalidator;
    // bumped BEFORE every event-driven invalidation: a filler that read
    // the delegate before the event and would install its (now stale)
    // body after it re-checks the epoch and self-invalidates. Global on
    // purpose - a false extra miss is harmless, a stale hit is not.
    private final AtomicLong invalidationEpoch = new AtomicLong();
    private volatile boolean closed;

    @Activate
    public NearCachingSegmentCache(
            @Reference(name = "delegate", target = "(daanse.segmentcache.hidden=true)") SegmentCache delegate,
            Config config) {
        this(delegate, config.maxWeightCells(), config.expireSeconds());
    }

    public NearCachingSegmentCache(SegmentCache delegate, long maxWeightCells) {
        this(delegate, maxWeightCells, DEFAULT_EXPIRE_SECONDS);
    }

    public NearCachingSegmentCache(SegmentCache delegate, long maxWeightCells, long expireSeconds) {
        this.delegate = delegate;
        this.near = Caffeine.newBuilder()
                .maximumWeight(maxWeightCells)
                .expireAfterWrite(Duration.ofSeconds(expireSeconds))
                .recordStats()
                .<SegmentHeader, SegmentBody>weigher((key, body) -> Math.max(1, body.cellCount()))
                .build();
        this.invalidator = event -> {
            // CREATED evicts too: a foreign put overwrites the body under
            // an existing id (rename targets, re-publishes), and a stale
            // near entry would shadow the fresh delegate body until
            // expiry. Evict, never preload - nothing is preloaded here.
            // Epoch first, THEN invalidate: a concurrent filler re-checks
            // the epoch after its put, so either order of arrival evicts.
            // Only FOREIGN events bump the epoch - the delegate fires its
            // local events synchronously INSIDE our own put/remove call,
            // strictly before the near write, so ordering already covers
            // them; bumping there would evict our own fresh put.
            if (!event.isLocal()) {
                invalidationEpoch.incrementAndGet();
            }
            near.invalidate(event.getSource());
        };
        delegate.addListener(invalidator);
    }

    /** Read side of recordStats: near hits versus delegate fetches. */
    public CacheStats stats() {
        return near.stats();
    }

    @Override
    public SegmentBody get(SegmentHeader header) {
        if (closed) {
            return null;
        }
        SegmentBody local = near.getIfPresent(header);
        if (local != null) {
            return local;
        }
        long epoch = invalidationEpoch.get();
        SegmentBody remote = delegate.get(header);
        if (remote != null) {
            near.put(header, remote);
            if (invalidationEpoch.get() != epoch) {
                // a foreign delete/overwrite landed while we read the
                // delegate: the body just installed may predate it
                near.invalidate(header);
            }
        }
        return remote;
    }

    @Override
    public boolean put(SegmentHeader header, SegmentBody body) {
        if (closed) {
            return false;
        }
        long epoch = invalidationEpoch.get();
        boolean stored = delegate.put(header, body);
        if (stored) {
            near.put(header, body);
            if (invalidationEpoch.get() != epoch) {
                // same window as in get(): a foreign event raced the
                // delegate round-trip
                near.invalidate(header);
            }
        }
        return stored;
    }

    @Override
    public boolean remove(SegmentHeader header) {
        // invalidate on BOTH sides of the delegate: a get racing between a
        // single leading invalidate and the delegate's remove re-populated
        // the near cache with the flushed body and served it until expiry
        near.invalidate(header);
        boolean removed = delegate.remove(header);
        // bump AFTER the delegate remove, BEFORE the trailing invalidate:
        // a filler that sampled the epoch earlier and puts after the
        // trailing invalidate then self-invalidates; one that samples
        // later can only have read the delegate post-remove (a miss).
        // The foreign-event bump in the listener does not cover this
        // window - the local delegate event fires inside THIS call, but a
        // concurrent get on another thread interleaves with it freely.
        invalidationEpoch.incrementAndGet();
        near.invalidate(header);
        return removed;
    }

    @Override
    public boolean rename(SegmentHeader oldHeader, SegmentHeader newHeader) {
        // the new header excludes cells; never serve the old body locally.
        // Same double invalidation as remove - see there. The NEW header is
        // invalidated too: a near entry already sitting under the target id
        // (rename chains, collision targets) would shadow the post-rename
        // delegate body.
        near.invalidate(oldHeader);
        near.invalidate(newHeader);
        boolean renamed = delegate.rename(oldHeader, newHeader);
        // same post-delegate bump as remove(): closes the local filler
        // window for both ids
        invalidationEpoch.incrementAndGet();
        near.invalidate(oldHeader);
        near.invalidate(newHeader);
        return renamed;
    }

    @Override
    public Set<StarKey> knownStars() {
        return delegate.knownStars();
    }

    @Override
    public List<SegmentHeader> getSegmentHeaders(
            ByteString schemaChecksum,
            String rolapStarFactTableName) {
        if (closed) {
            return List.of();
        }
        return delegate.getSegmentHeaders(schemaChecksum, rolapStarFactTableName);
    }

    @Override
    public List<SegmentHeader> getSegmentHeaders() {
        if (closed) {
            return List.of();
        }
        return delegate.getSegmentHeaders(); // inventory stays with the delegate
    }

    /** Closes this layer only; the delegate belongs to its provider. */
    @Deactivate
    @Override
    public void tearDown() {
        closed = true;
        delegate.removeListener(invalidator);
        near.invalidateAll();
    }

    @Override
    public void addListener(SegmentCacheListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(SegmentCacheListener listener) {
        delegate.removeListener(listener);
    }

}
