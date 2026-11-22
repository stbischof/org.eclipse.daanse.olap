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
package org.eclipse.daanse.olap.calc.base.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.eclipse.daanse.olap.api.ContextConfig;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.calc.profile.ProfileCounters;
import org.eclipse.daanse.olap.api.calc.profile.ProfileHandler;
import org.eclipse.daanse.olap.api.calc.profile.ProfilingControl;
import org.eclipse.daanse.olap.api.calc.profile.ProfilingPolicy;
import org.eclipse.daanse.olap.api.calc.profile.ProfilingPolicy.Mode;
import org.eclipse.daanse.olap.api.calc.profile.ProfilingPolicy.Sink;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.execution.QueryTiming;
import org.eclipse.daanse.olap.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The profiling scopes of one context: connection over cube over context, decided once per statement before it
 * compiles. Shared by every art. A statement that is not profiled pays one map lookup and two tag reads.
 */
public final class ProfilingScopes implements ProfilingControl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilingScopes.class);

    public static final String TAG_MODE = "daanse:profile.mode";
    public static final String TAG_SAMPLE = "daanse:profile.sample";
    public static final String TAG_SINK = "daanse:profile.sink";
    public static final String TAG_DETAIL = "daanse:profile.detail";
    public static final String TAG_TOP = "daanse:profile.top";

    /** Plans of a scope with the log sink: INFO on daanse.profile, with the connection. */
    private static ProfileHandler logSink(long connectionId) {
        return (plan, timing) -> Util.PROFILE_LOGGER.info("connection {}: {}{}", connectionId, plan,
                timing == null ? "" : "\n" + timing);
    }

    /** The profile logger at DEBUG profiles every statement, as before the scopes. */
    private static final ProfileHandler DEBUG_LOG = (plan, timing) -> Util.PROFILE_LOGGER
            .debug(timing == null ? plan : plan + "\n" + timing);

    private final Supplier<ContextConfig> config;
    private final Map<Long, ProfilingPolicy> connections = new ConcurrentHashMap<>();
    /** The handlers set for single connections, which take their plans in place of the sink. */
    private final Map<Long, ProfileHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> samples = new ConcurrentHashMap<>();
    private volatile ProfileHandler sink;
    private final ProfileCounters counters = new ProfileCounters();

    /** The handler of the counters sink: no plan, the hulls count into the context's counters themselves. */
    private final ProfileHandler counting = new ProfileHandler() {
        @Override
        public void explain(String plan, QueryTiming timing) {
            // the counters are the output
        }

        @Override
        public void explain(String title, CalculationProfile profile, QueryTiming timing) {
            // the counters are the output
        }

        @Override
        public ProfileCounters counters() {
            return counters;
        }
    };

    public ProfilingScopes(Supplier<ContextConfig> config) {
        this.config = config;
    }

    @Override
    public void set(long connectionId, ProfilingPolicy policy) {
        connections.put(connectionId, policy);
        handlers.remove(connectionId);
        samples.remove("connection:" + connectionId);
    }

    @Override
    public void set(long connectionId, ProfilingPolicy policy, ProfileHandler handler) {
        connections.put(connectionId, policy);
        handlers.put(connectionId, handler);
        samples.remove("connection:" + connectionId);
    }

    @Override
    public void clear(long connectionId) {
        connections.remove(connectionId);
        handlers.remove(connectionId);
        samples.remove("connection:" + connectionId);
    }

    @Override
    public ProfilingPolicy policy(long connectionId) {
        return connections.getOrDefault(connectionId, ProfilingPolicy.UNSET);
    }

    @Override
    public void setSink(ProfileHandler sink) {
        this.sink = sink;
    }

    @Override
    public ProfileHandler handlerFor(Connection connection, Cube cube) {
        long id = connection == null ? -1 : connection.getId();
        ProfilingPolicy own = policy(id);
        ProfilingPolicy tagged = cube == null ? ProfilingPolicy.UNSET
                : ofTags(cube.getCatalog() == null ? null : cube.getCatalog().getMetaData(), cube.getMetaData());
        ProfilingPolicy context = ofConfig(config.get());
        String owner = own.mode() != Mode.INHERIT ? "connection:" + id
                : tagged.mode() != Mode.INHERIT ? "cube:" + cube.getUniqueName() : "context";
        ProfilingPolicy effective = own.over(tagged.over(context));
        boolean profiled = switch (effective.mode()) {
        case ON -> true;
        case SAMPLE -> samples.computeIfAbsent(owner, k -> new AtomicLong()).incrementAndGet()
                % effective.sampleEvery() == 0;
        case OFF, INHERIT -> false;
        };
        if (!profiled) {
            return effective.mode() == Mode.OFF || !Util.PROFILE_LOGGER.isDebugEnabled() ? null : DEBUG_LOG;
        }
        ProfileHandler handler = switch (effective.sink()) {
        case COUNTERS -> counting;
        case HANDLER -> own.mode() != Mode.INHERIT && handlers.containsKey(id) ? handlers.get(id)
                : sink != null ? sink : logSink(id);
        case LOG -> logSink(id);
        };
        return effective.detail() == ProfilingPolicy.Detail.TOP && effective.sink() != Sink.COUNTERS
                ? top(handler, effective.topN()) : handler;
    }

    @Override
    public ProfileCounters counters() {
        return counters;
    }

    /**
     * The handler, shown only the {@code n} calcs of each plan that took the most time of their own: a flat list under
     * the root, each without its children, the most expensive first. A plan of hundreds of calcs is read for the few
     * that cost.
     */
    static ProfileHandler top(ProfileHandler handler, int n) {
        return new ProfileHandler() {
            @Override
            public void explain(String plan, QueryTiming timing) {
                handler.explain(plan, timing);
            }

            @Override
            public void explain(String title, CalculationProfile profile, QueryTiming timing) {
                handler.explain(title, profile == null ? null : top(profile, n), timing);
            }

            @Override
            public ProfileCounters counters() {
                return handler.counters();
            }
        };
    }

    /** The root of the tree with the {@code n} calcs of the whole tree that took the most time of their own. */
    static CalculationProfile top(CalculationProfile root, int n) {
        List<CalculationProfile> all = new ArrayList<>();
        collect(root, all);
        all.sort(Comparator.comparingLong(CalculationProfile::ownNanos).reversed());
        List<CalculationProfile> kept = new ArrayList<>(Math.min(n, all.size()));
        for (CalculationProfile profile : all.subList(0, Math.min(n, all.size()))) {
            kept.add(new CalcProfileR(profile.clazz(), profile.name(), profile.type(), profile.resultStyle(),
                    profile.nanos(), profile.ownNanos(), profile.elements(), profile.elementsSquared(),
                    profile.additionalValues(), List.of()));
        }
        return new CalcProfileR(root.clazz(), java.util.Optional.of("top " + n + " by own time"), root.type(),
                root.resultStyle(), root.nanos(), root.ownNanos(), root.elements(), root.elementsSquared(),
                Map.of(), List.copyOf(kept));
    }

    private static void collect(CalculationProfile profile, List<CalculationProfile> into) {
        into.add(profile);
        for (CalculationProfile child : profile.childProfiles()) {
            collect(child, into);
        }
    }

    /** The context-wide policy from the configuration. */
    static ProfilingPolicy ofConfig(ContextConfig config) {
        if (config == null) {
            return ProfilingPolicy.off();
        }
        return parse("profileMode", config.profileMode(), String.valueOf(config.profileSampleEvery()),
                config.profileSink(), config.profileDetail(), String.valueOf(config.profileTopN()),
                ProfilingPolicy.off());
    }

    /** A cube's policy from its tags over its catalog's; nothing tagged says nothing. */
    static ProfilingPolicy ofTags(MetaData catalog, MetaData cube) {
        ProfilingPolicy ofCatalog = parse(TAG_MODE, tag(catalog, TAG_MODE), tag(catalog, TAG_SAMPLE),
                tag(catalog, TAG_SINK), tag(catalog, TAG_DETAIL), tag(catalog, TAG_TOP), ProfilingPolicy.UNSET);
        ProfilingPolicy ofCube = parse(TAG_MODE, tag(cube, TAG_MODE), tag(cube, TAG_SAMPLE), tag(cube, TAG_SINK),
                tag(cube, TAG_DETAIL), tag(cube, TAG_TOP), ProfilingPolicy.UNSET);
        return ofCube.over(ofCatalog);
    }

    private static String tag(MetaData meta, String tag) {
        Object value = meta == null ? null : meta.get(tag);
        return value == null ? null : String.valueOf(value);
    }

    /** off, on, sample (with every) — anything else warns and falls back; detail top keeps the top n calcs. */
    private static ProfilingPolicy parse(String what, String mode, String every, String sink, String detail,
            String top, ProfilingPolicy fallback) {
        ProfilingPolicy policy = parse(what, mode, every, sink, fallback);
        if (policy.mode() == Mode.INHERIT || policy.mode() == Mode.OFF || !"top".equalsIgnoreCase(detail)) {
            return policy;
        }
        int n = ProfilingPolicy.DEFAULT_TOP_N;
        try {
            int given = Integer.parseInt(String.valueOf(top).trim());
            if (given >= 1) {
                n = given;
            }
        } catch (NumberFormatException e) {
            // the default
        }
        return policy.top(n);
    }

    private static ProfilingPolicy parse(String what, String mode, String every, String sink,
            ProfilingPolicy fallback) {
        if (mode == null) {
            return fallback;
        }
        Sink target = "handler".equalsIgnoreCase(sink) ? Sink.HANDLER
                : "counters".equalsIgnoreCase(sink) ? Sink.COUNTERS : Sink.LOG;
        switch (mode.toLowerCase(Locale.ROOT)) {
        case "off":
            return ProfilingPolicy.off();
        case "on":
            return ProfilingPolicy.on(target);
        case "sample":
            try {
                int n = Integer.parseInt(String.valueOf(every).trim());
                if (n >= 1) {
                    return ProfilingPolicy.sample(n, target);
                }
            } catch (NumberFormatException e) {
                // below
            }
            LOGGER.warn("{} is sample but the sample size '{}' is no positive number; profiling stays off", what,
                    every);
            return ProfilingPolicy.off();
        default:
            LOGGER.warn("{} carries '{}' (expected off, on or sample); {}", what, mode,
                    fallback.mode() == Mode.INHERIT ? "the wider scope decides" : "profiling stays off");
            return fallback;
        }
    }
}
