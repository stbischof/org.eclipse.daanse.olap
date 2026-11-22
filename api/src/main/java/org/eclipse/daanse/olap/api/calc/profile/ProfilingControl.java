/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.api.calc.profile;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Cube;

/**
 * Profiling of a context from the inside, for monitoring: which connections are profiled, where the plans go, and the
 * decision for one statement. The context-wide policy comes from the configuration ({@code profileMode},
 * {@code profileSampleEvery}, {@code profileSink}), a cube's from its tags {@code daanse:profile.mode},
 * {@code daanse:profile.sample}, {@code daanse:profile.sink} (the catalog's tags are the default of its cubes).
 */
public interface ProfilingControl {

    /** Profiles the statements of one connection by this policy, over the cube's and the context's. */
    void set(long connectionId, ProfilingPolicy policy);

    /**
     * Profiles the statements of one connection by this policy into a handler of its own: with
     * {@link ProfilingPolicy.Sink#HANDLER} the plans of this connection go there and not to the context's
     * {@link #setSink(ProfileHandler) sink}. For a caller that has to know whose plan it reads -- a server trace
     * reporting each plan with the session it ran in -- since a plan does not name its connection.
     */
    void set(long connectionId, ProfilingPolicy policy, ProfileHandler handler);

    /** Forgets the policy of the connection; also done when the connection closes. */
    void clear(long connectionId);

    /** The policy set for the connection, {@link ProfilingPolicy#UNSET} if none. */
    ProfilingPolicy policy(long connectionId);

    /** What the statements profiled with {@link ProfilingPolicy.Sink#COUNTERS} counted, over all of them. */
    ProfileCounters counters();

    /** The handler for {@link ProfilingPolicy.Sink#HANDLER}; without one, such plans go to the log. */
    void setSink(ProfileHandler sink);

    /**
     * The handler a new statement on the connection, querying the cube, is profiled into, or {@code null} if it is not
     * profiled. Decided once per statement, before it compiles; a {@link ProfilingPolicy.Mode#SAMPLE} scope counts
     * the statement here.
     */
    ProfileHandler handlerFor(Connection connection, Cube cube);
}
