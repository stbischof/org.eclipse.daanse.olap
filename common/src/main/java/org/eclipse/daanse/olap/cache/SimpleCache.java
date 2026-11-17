/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2004-2005 TONBELLER AG
 * Copyright (C) 2006-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.cache;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiFunction;

/**
 * Defines a cache API. Implementations exist for softly referenced values
 * (the relational engine's {@code SoftValueCache}) and bounded strong
 * references ({@link BoundedCache}).
 *
 * To iterate over the contents of a cache, pass a task to
 * {@link #execute(Task)}. The iteration is weakly consistent and
 * per-entry atomic - it is not a frozen snapshot and does not lock
 * out concurrent writers.
 *
 * Implementations are responsible of enforcing thread safety.
 * @author av
 * @since Nov 21, 2005
 */
public interface SimpleCache <K, V> {
    /**
     * Places a key/value pair into the cache. The value must not be null;
     * use {@link #remove} to drop an entry.
     *
     * @param key Key
     * @param value Value
     * @return the previous value of key or null
     */
    V put(K key, V value);

    /**
     * Places a key/value pair into the cache unless the key is already
     * present. The value must not be null.
     *
     * @return the previously associated value, or null if this call stored
     *         the given value
     */
    V putIfAbsent(K key, V value);

    /**
     * Atomically merges a value into an entry: stores {@code value} on a
     * miss, otherwise replaces the entry with
     * {@code remapping.apply(existing, value)} (a null result removes it).
     *
     * @return the value now associated with the key, or null if removed
     */
    V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remapping);

    /**
     * Atomically replaces an entry only while it still holds
     * {@code oldValue}. Never inserts on a missing key - unlike
     * {@link #merge}, a lost race leaves the cache untouched, which makes
     * this the safe channel for shrinking an entry a concurrent writer
     * may refresh (see {@link Task}).
     *
     * @return true when the entry was replaced
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Looks up and returns a cache value according to a given key.
     * If the cache does not correspond an entry corresponding to the key,
     * null is returned.
     */
    V get(K key);

    /**
     * Removes a key from the cache.
     *
     * @param key Key
     *
     * @return Previous value associated with the key
     */
    V remove(K key);

    /**
     * Clears the contents of this cache.
     */
    void clear();

    /**
     * Returns the number of entries; a reference-based cache may still
     * count entries whose value the garbage collector already cleared.
     * Test-observability only - no production caller (kept deliberately).
     */
    int size();


    /**
     * Executes a task over the contents of the cache. This guarantees
     * PER-ENTRY atomicity only, not exclusive write access: concurrent
     * writers (query-side loads) may add or refresh entries while the
     * task iterates - the iterated entries are point-in-time snapshots.
     * @param task The task to execute.
     */
    void execute(Task<K, V> task);

    /**
     * Defines a task to be run over the entries of the cache.
     * Used in conjunction with {@link #execute(Task)}.
     *
     * <p>Contract for the iterated entries: {@code iterator.remove()} is
     * supported by every implementation. {@code entry.setValue} is NOT part
     * of the common contract - {@code SoftValueCache} yields immutable
     * snapshot entries (setValue throws), while {@link BoundedCache} yields
     * write-through entries whose setValue is an unconditional put: it can
     * RESURRECT an entry the cache evicted, and it silently discards a
     * value a concurrent writer put between {@code next()} and the call.
     * To rewrite an entry from inside a task, use
     * {@link SimpleCache#replace(Object, Object, Object)} with the
     * iterated value as the expected one, and treat a failed replace as
     * "a concurrent writer got here first".</p>
     */
    public interface Task<K, V> {
        void execute(
            Iterator<Entry<K, V>> iterator);
    }
}
