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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentCache;
import org.eclipse.daanse.olap.spi.SegmentHeader;
import org.eclipse.daanse.olap.segmentcache.common.SegmentCodec;

/** Map-backed delegate fixture; counts gets and fires local events. */
class MapSegmentCache implements SegmentCache {

    final Map<String, SegmentBody> bodies = new ConcurrentHashMap<>();
    final Map<String, SegmentHeader> headers = new ConcurrentHashMap<>();
    final AtomicInteger gets = new AtomicInteger();
    private final List<SegmentCacheListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public SegmentBody get(SegmentHeader header) {
        gets.incrementAndGet();
        return bodies.get(SegmentCodec.key(header).value());
    }

    @Override
    public boolean put(SegmentHeader header, SegmentBody body) {
        String key = SegmentCodec.key(header).value();
        bodies.put(key, body);
        headers.put(key, header);
        fire(header, SegmentCacheListener.SegmentCacheEvent.EventType.ENTRY_CREATED);
        return true;
    }

    @Override
    public boolean remove(SegmentHeader header) {
        String key = SegmentCodec.key(header).value();
        headers.remove(key);
        boolean removed = bodies.remove(key) != null;
        if (removed) {
            fire(header, SegmentCacheListener.SegmentCacheEvent.EventType.ENTRY_DELETED);
        }
        return removed;
    }

    @Override
    public List<SegmentHeader> getSegmentHeaders() {
        return new ArrayList<>(headers.values());
    }

    @Override
    public void tearDown() {
        bodies.clear();
        headers.clear();
        listeners.clear();
    }

    @Override
    public void addListener(SegmentCacheListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SegmentCacheListener listener) {
        listeners.remove(listener);
    }


    void fire(SegmentHeader header, SegmentCacheListener.SegmentCacheEvent.EventType type) {
        fire(header, type, true);
    }

    /** A cross-node event, the way a Redis/Hazelcast subscription delivers it. */
    void fireForeign(SegmentHeader header, SegmentCacheListener.SegmentCacheEvent.EventType type) {
        fire(header, type, false);
    }

    private void fire(SegmentHeader header, SegmentCacheListener.SegmentCacheEvent.EventType type,
            boolean local) {
        SegmentCacheListener.SegmentCacheEvent event = new SegmentCacheListener.SegmentCacheEvent() {
            @Override
            public EventType getEventType() {
                return type;
            }

            @Override
            public SegmentHeader getSource() {
                return header;
            }

            @Override
            public boolean isLocal() {
                return local;
            }
        };
        for (SegmentCacheListener listener : listeners) {
            listener.handle(event);
        }
    }
}
