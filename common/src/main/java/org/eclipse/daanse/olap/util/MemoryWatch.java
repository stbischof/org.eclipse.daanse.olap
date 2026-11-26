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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.util;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.execution.Execution;

/**
 * Watches the heap while one execution runs: past the configured share ({@code memoryMonitorThreshold}),
 * the execution is told it may run out of memory, and its next cancel check ends it with a message instead
 * of letting the whole JVM fail. Switched by {@code memoryMonitor}.
 *
 * <p>
 * The watch knows nothing of SQL: what fills the heap is an intermediate set or a result in any art.
 *
 * <p>
 * One notifying monitor serves the JVM: it watches the tenured pool, which there is once, and each new
 * monitor would register one more listener with the platform.
 */
public final class MemoryWatch implements AutoCloseable {

    private static final class Shared {
        static final MemoryMonitor MONITOR = new NotificationMemoryMonitor();
    }

    /**
     * What a watch also does when the heap passes the share: caches that can drop what they hold give it back (the
     * member budgets of the catalogs). Held weakly -- a catalog that is gone takes its relief with it.
     */
    private static final java.util.Set<Runnable> RELIEFS =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    /**
     * Registers a relief to run under memory pressure; the caller keeps the {@code Runnable} reachable as long as it
     * shall run (it is held weakly).
     */
    public static void relieveUnderPressure(Runnable relief) {
        RELIEFS.add(relief);
    }

    /** Runs every registered relief; what the listener of a watch does past the share. */
    public static void relieve() {
        java.util.List<Runnable> now;
        synchronized (RELIEFS) {
            now = new java.util.ArrayList<>(RELIEFS);
        }
        now.forEach(Runnable::run);
    }

    private final MemoryMonitor monitor;
    private final MemoryMonitor.Listener listener;

    private MemoryWatch(MemoryMonitor monitor, MemoryMonitor.Listener listener) {
        this.monitor = monitor;
        this.listener = listener;
    }

    /**
     * Starts watching for this execution as the context is configured, and checks at once: a heap already
     * past the share ends the execution before it starts its work.
     *
     * @param what names the query in the message; never the connect string, which can carry a password
     */
    public static MemoryWatch during(Context<?> context, Execution execution, String what) {
        if (!context.getConfig().memoryMonitor()) {
            return new MemoryWatch(null, null);
        }
        return during(Shared.MONITOR, context.getConfig().memoryMonitorThreshold(), execution, what);
    }

    /** Starts watching with the given monitor; the seam for a test. */
    public static MemoryWatch during(MemoryMonitor monitor, int thresholdPercentage, Execution execution,
            String what) {
        MemoryMonitor.Listener listener = (used, max) -> {
            relieve();
            execution.setOutOfMemory(
                new StringBuilder("OutOfMemory used=").append(used).append(", max=").append(max)
                        .append(" for query: ").append(what).toString());
        };
        monitor.addListener(listener, thresholdPercentage);
        try {
            execution.checkCancelOrTimeout();
        } catch (RuntimeException ended) {
            monitor.removeListener(listener);
            throw ended;
        }
        return new MemoryWatch(monitor, listener);
    }

    @Override
    public void close() {
        if (monitor != null) {
            monitor.removeListener(listener);
        }
    }
}
