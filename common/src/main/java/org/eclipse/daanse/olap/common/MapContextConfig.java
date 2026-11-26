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
package org.eclipse.daanse.olap.common;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.daanse.olap.api.ContextConfig;

/**
 * {@link ContextConfig} over the configuration map a context was activated with.
 *
 * <p>
 * The map is read on every call rather than copied once, because it is replaced
 * when OSGi Configuration Admin updates the component and mutated in place by the
 * test contexts. A snapshot taken at construction would go stale in both cases.
 * Reads are cheap: a hash lookup and a type check.
 * </p>
 *
 * <p>
 * Every getter names its key and its default from {@link ConfigConstants}, so
 * those constants stay the single place where either is written down.
 * </p>
 */
public class MapContextConfig implements ContextConfig {

    private final Supplier<Map<String, Object>> configuration;

    /**
     * @param configuration supplies the current configuration map, or null when
     *                      the context has none; every getter then answers with
     *                      its default
     */
    public MapContextConfig(Supplier<Map<String, Object>> configuration) {
        this.configuration = configuration;
    }

    /**
     * Reads one value, falling back to {@code dflt} whenever the store holds
     * nothing usable for {@code key}.
     *
     * <p>
     * A string is parsed into the expected type: a configuration file, a
     * {@code .cfg} without typed syntax and most management agents deliver numbers
     * and flags as text. Text that does not parse falls back to the default rather
     * than failing the context.
     * </p>
     */
    private <T> T value(String key, T dflt, Class<T> type) {
        Map<String, Object> config = configuration.get();
        Object value = config == null ? null : config.get(key);
        if (value == null) {
            return dflt;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof Number number) {
            if (type == Integer.class) {
                return type.cast(number.intValue());
            }
            if (type == Long.class) {
                return type.cast(number.longValue());
            }
            if (type == Double.class) {
                return type.cast(number.doubleValue());
            }
        }
        if (value instanceof String text && !text.isBlank()) {
            return parse(text.trim(), dflt, type);
        }
        return dflt;
    }

    private <T> T parse(String text, T dflt, Class<T> type) {
        try {
            if (type == Boolean.class) {
                return type.cast(Boolean.valueOf(text));
            }
            if (type == Integer.class) {
                return type.cast(Integer.valueOf(text));
            }
            if (type == Long.class) {
                return type.cast(Long.valueOf(text));
            }
            if (type == Double.class) {
                return type.cast(Double.valueOf(text));
            }
        } catch (NumberFormatException e) {
            return dflt;
        }
        return dflt;
    }

    /**
     * Reads a {@link TimeUnit} that may be stored either as a constant or as its
     * name.
     *
     * <p>
     * Configuration Admin and property files carry strings, so both forms have to
     * be accepted. An unusable name falls back to the default rather than failing
     * the whole context.
     * </p>
     */
    private TimeUnit timeUnit(String key, String dflt) {
        Map<String, Object> config = configuration.get();
        Object value = config == null ? null : config.get(key);
        if (value instanceof TimeUnit unit) {
            return unit;
        }
        String name = value instanceof String s && !s.isBlank() ? s : dflt;
        try {
            return TimeUnit.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TimeUnit.valueOf(dflt);
        }
    }

    // --- native evaluation --------------------------------------------

    @Override
    public boolean enableNativeFilter() {
        return value(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean enableNativeCrossJoin() {
        return value(ConfigConstants.ENABLE_NATIVE_CROSS_JOIN, ConfigConstants.ENABLE_NATIVE_CROSS_JOIN_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean enableNativeNonEmpty() {
        return value(ConfigConstants.ENABLE_NATIVE_NON_EMPTY, ConfigConstants.ENABLE_NATIVE_NON_EMPTY_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean enableNativeTopCount() {
        return value(ConfigConstants.ENABLE_NATIVE_TOP_COUNT, ConfigConstants.ENABLE_NATIVE_TOP_COUNT_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean expandNonNative() {
        return value(ConfigConstants.EXPAND_NON_NATIVE, ConfigConstants.EXPAND_NON_NATIVE_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public int nativizeMinThreshold() {
        return value(ConfigConstants.NATIVIZE_MIN_THRESHOLD, ConfigConstants.NATIVIZE_MIN_THRESHOLD_DEFAULT_VALUE,
                Integer.class);
    }

    @Override
    public int nativizeMaxResults() {
        return value(ConfigConstants.NATIVIZE_MAX_RESULTS, ConfigConstants.NATIVIZE_MAX_RESULTS_DEFAULT_VALUE,
                Integer.class);
    }

    @Override
    public String alertNativeEvaluationUnsupported() {
        return value(ConfigConstants.ALERT_NATIVE_EVALUATION_UNSUPPORTED,
                ConfigConstants.ALERT_NATIVE_EVALUATION_UNSUPPORTED_DEFAULT_VALUE, String.class);
    }

    @Override
    public int levelPreCacheThreshold() {
        return value(ConfigConstants.LEVEL_PRE_CACHE_THRESHOLD,
                ConfigConstants.LEVEL_PRE_CACHE_THRESHOLD_DEFAULT_VALUE, Integer.class);
    }

    // --- cell cache and segments --------------------------------------


    @Override
    public boolean disableCaching() {
        return value(ConfigConstants.DISABLE_CACHING, ConfigConstants.DISABLE_CACHING_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean disableLocalSegmentCache() {
        return value(ConfigConstants.DISABLE_LOCAL_SEGMENT_CACHE,
                ConfigConstants.DISABLE_LOCAL_SEGMENT_CACHE_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean enableSessionCaching() {
        return value(ConfigConstants.ENABLE_SESSION_CACHING, ConfigConstants.ENABLE_SESSION_CACHING_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public int cellBatchSize() {
        return value(ConfigConstants.CELL_BATCH_SIZE, ConfigConstants.CELL_BATCH_SIZE_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int sparseSegmentCountThreshold() {
        return value(ConfigConstants.SPARSE_SEGMENT_COUNT_THRESHOLD,
                ConfigConstants.SPARSE_SEGMENT_COUNT_THRESHOLD_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public double sparseSegmentDensityThreshold() {
        return value(ConfigConstants.SPARSE_SEGMENT_DENSITY_THRESHOLD,
                ConfigConstants.SPARSE_SEGMENT_DENSITY_THRESHOLD_DEFAULT_VALUE, Double.class);
    }

    @Override
    public boolean enableInMemoryRollup() {
        return value(ConfigConstants.ENABLE_IN_MEMORY_ROLLUP, ConfigConstants.ENABLE_IN_MEMORY_ROLLUP_DEFAULT_VALUE,
                Boolean.class);
    }

    // --- evaluation and MDX semantics ---------------------------------

    @Override
    public String solveOrderMode() {
        return value(ConfigConstants.SOLVE_ORDER_MODE, ConfigConstants.SOLVE_ORDER_MODE_DEFAULT_VALUE, String.class);
    }

    @Override
    public int compoundSlicerMemberSolveOrder() {
        return value(ConfigConstants.COMPOUND_SLICER_MEMBER_SOLVE_ORDER,
                ConfigConstants.COMPOUND_SLICER_MEMBER_SOLVE_ORDER_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public String currentMemberWithCompoundSlicerAlert() {
        return value(ConfigConstants.CURRENT_MEMBER_WITH_COMPOUND_SLICER_ALERT,
                ConfigConstants.CURRENT_MEMBER_WITH_COMPOUND_SLICER_ALERT_DEFAULT_VALUE, String.class);
    }

    @Override
    public boolean ignoreMeasureForNonJoiningDimension() {
        return value(ConfigConstants.IGNORE_MEASURE_FOR_NON_JOINING_DIMENSION,
                ConfigConstants.IGNORE_MEASURE_FOR_NON_JOINING_DIMENSION_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean nullDenominatorProducesNull() {
        return value(ConfigConstants.NULL_DENOMINATOR_PRODUCES_NULL,
                ConfigConstants.NULL_DENOMINATOR_PRODUCES_NULL_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean needDimensionPrefix() {
        return value(ConfigConstants.NEED_DIMENSION_PREFIX, ConfigConstants.NEED_DIMENSION_PREFIX_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean ignoreInvalidMembers() {
        return value(ConfigConstants.IGNORE_INVALID_MEMBERS, ConfigConstants.IGNORE_INVALID_MEMBERS_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean ignoreInvalidMembersDuringQuery() {
        return value(ConfigConstants.IGNORE_INVALID_MEMBERS_DURING_QUERY,
                ConfigConstants.IGNORE_INVALID_MEMBERS_DURING_QUERY_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public int maxEvalDepth() {
        return value(ConfigConstants.MAX_EVAL_DEPTH, ConfigConstants.MAX_EVAL_DEPTH_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int iterationLimit() {
        return value(ConfigConstants.ITERATION_LIMIT, ConfigConstants.ITERATION_LIMIT_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int crossJoinOptimizerSize() {
        return value(ConfigConstants.CROSS_JOIN_OPTIMIZER_SIZE,
                ConfigConstants.CROSS_JOIN_OPTIMIZER_SIZE_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public boolean optimizePredicates() {
        return value(ConfigConstants.OPTIMIZE_PREDICATES, ConfigConstants.OPTIMIZE_PREDICATES_DEFAULT_VALUE,
                Boolean.class);
    }

    // --- aggregate tables ---------------------------------------------

    @Override
    public boolean useAggregates() {
        return value(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean readAggregates() {
        return value(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean chooseAggregateByVolume() {
        return value(ConfigConstants.CHOOSE_AGGREGATE_BY_VOLUME,
                ConfigConstants.CHOOSE_AGGREGATE_BY_VOLUME_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean generateAggregateSql() {
        return value(ConfigConstants.GENERATE_AGGREGATE_SQL, ConfigConstants.GENERATE_AGGREGATE_SQL_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean enableGroupingSets() {
        return value(ConfigConstants.ENABLE_GROUPING_SETS, ConfigConstants.ENABLE_GROUPING_SETS_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean enableTotalCount() {
        return value(ConfigConstants.ENABLE_TOTAL_COUNT, ConfigConstants.ENABLE_TOTAL_COUNT_DEFAULT_VALUE,
                Boolean.class);
    }

    // --- concurrency ---------------------------------------------------

    @Override
    public int queryLimit() {
        return value(ConfigConstants.QUERY_LIMIT, ConfigConstants.QUERY_LIMIT_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int rolapConnectionShepherdNbThreads() {
        return value(ConfigConstants.ROLAP_CONNECTION_SHEPHERD_NB_THREADS,
                ConfigConstants.ROLAP_CONNECTION_SHEPHERD_NB_THREADS_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public long rolapConnectionShepherdThreadPollingInterval() {
        return value(ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL,
                ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_DEFAULT_VALUE, Long.class);
    }

    @Override
    public TimeUnit rolapConnectionShepherdThreadPollingIntervalUnit() {
        return timeUnit(ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT,
                ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT_DEFAULT_VALUE);
    }

    @Override
    public int segmentCacheManagerNumberSqlThreads() {
        return value(ConfigConstants.SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS,
                ConfigConstants.SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int segmentCacheManagerNumberCacheThreads() {
        return value(ConfigConstants.SEGMENT_CACHE_MANAGER_NUMBER_CACHE_THREADS,
                ConfigConstants.SEGMENT_CACHE_MANAGER_NUMBER_CACHE_THREADS_DEFAULT_VALUE, Integer.class);
    }

    // --- execution, cancellation and limits ---------------------------

    @Override
    public int queryTimeout() {
        return value(ConfigConstants.QUERY_TIMEOUT, ConfigConstants.QUERY_TIMEOUT_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public long executeDuration() {
        return value(ConfigConstants.EXECUTE_DURATION, ConfigConstants.EXECUTE_DURATION_DEFAULT_VALUE, Long.class);
    }

    @Override
    public TimeUnit executeDurationUnit() {
        return timeUnit(ConfigConstants.EXECUTE_DURATION_UNIT, ConfigConstants.EXECUTE_DURATION_UNIT_DEFAULT_VALUE);
    }

    @Override
    public int checkCancelOrTimeoutInterval() {
        return value(ConfigConstants.CHECK_CANCEL_OR_TIMEOUT_INTERVAL,
                ConfigConstants.CHECK_CANCEL_OR_TIMEOUT_INTERVAL_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public boolean enableDrillThrough() {
        return value(ConfigConstants.ENABLE_DRILL_THROUGH, ConfigConstants.ENABLE_DRILL_THROUGH_DEFAULT_VALUE,
                Boolean.class);
    }

    // --- memory monitor ------------------------------------------------

    @Override
    public boolean memoryMonitor() {
        return value(ConfigConstants.MEMORY_MONITOR, ConfigConstants.MEMORY_MONITOR_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public int memoryMonitorThreshold() {
        return value(ConfigConstants.MEMORY_MONITOR_THRESHOLD, ConfigConstants.MEMORY_MONITOR_THRESHOLD_DEFAULT_VALUE,
                Integer.class);
    }

    // --- SQL and diagnostics -------------------------------------------

    @Override
    public boolean generateFormattedSql() {
        return value(ConfigConstants.GENERATE_FORMATTED_SQL, ConfigConstants.GENERATE_FORMATTED_SQL_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public String warnIfNoPatternForDialect() {
        return value(ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT,
                ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT_DEFAULT_VALUE, String.class);
    }

    @Override
    public int testExpDependencies() {
        return value(ConfigConstants.TEST_EXP_DEPENDENCIES, ConfigConstants.TEST_EXP_DEPENDENCIES_DEFAULT_VALUE,
                Integer.class);
    }

    @Override
    public String profileMode() {
        return value(ConfigConstants.PROFILE_MODE, ConfigConstants.PROFILE_MODE_DEFAULT_VALUE, String.class);
    }

    @Override
    public int profileSampleEvery() {
        return value(ConfigConstants.PROFILE_SAMPLE_EVERY, ConfigConstants.PROFILE_SAMPLE_EVERY_DEFAULT_VALUE,
                Integer.class);
    }

    @Override
    public String profileSink() {
        return value(ConfigConstants.PROFILE_SINK, ConfigConstants.PROFILE_SINK_DEFAULT_VALUE, String.class);
    }

    @Override
    public String profileDetail() {
        return value(ConfigConstants.PROFILE_DETAIL, ConfigConstants.PROFILE_DETAIL_DEFAULT_VALUE, String.class);
    }

    @Override
    public int profileTopN() {
        return value(ConfigConstants.PROFILE_TOP_N, ConfigConstants.PROFILE_TOP_N_DEFAULT_VALUE, Integer.class);
    }

    // --- naming, ordering and result shape -----------------------------

    @Override
    public boolean caseSensitive() {
        return value(ConfigConstants.CASE_SENSITIVE, ConfigConstants.CASE_SENSITIVE_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean caseSensitiveMdxInstr() {
        return value(ConfigConstants.CASE_SENSITIVE_MDX_INSTR, ConfigConstants.CASE_SENSITIVE_MDX_INSTR_DEFAULT_VALUE,
                Boolean.class);
    }

    @Override
    public boolean compareSiblingsByOrderKey() {
        return value(ConfigConstants.COMPARE_SIBLINGS_BY_ORDER_KEY,
                ConfigConstants.COMPARE_SIBLINGS_BY_ORDER_KEY_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean enableExpCache() {
        return value(ConfigConstants.ENABLE_EXP_CACHE, ConfigConstants.ENABLE_EXP_CACHE_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean enableNonEmptyOnAllAxis() {
        return value(ConfigConstants.ENABLE_NON_EMPTY_ON_ALL_AXIS,
                ConfigConstants.ENABLE_NON_EMPTY_ON_ALL_AXIS_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean enableRolapCubeMemberCache() {
        return value(ConfigConstants.ENABLE_ROLAP_CUBE_MEMBER_CACHE,
                ConfigConstants.ENABLE_ROLAP_CUBE_MEMBER_CACHE_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public boolean filterChildlessSnowflakeMembers() {
        return value(ConfigConstants.FILTER_CHILDLESS_SNOWFLAKE_MEMBERS,
                ConfigConstants.FILTER_CHILDLESS_SNOWFLAKE_MEMBERS_DEFAULT_VALUE, Boolean.class);
    }

    @Override
    public int maxConstraints() {
        return value(ConfigConstants.MAX_CONSTRAINTS, ConfigConstants.MAX_CONSTRAINTS_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public String nullMemberRepresentation() {
        return value(ConfigConstants.NULL_MEMBER_REPRESENTATION,
                ConfigConstants.NULL_MEMBER_REPRESENTATION_DEFAULT_VALUE, String.class);
    }

    @Override
    public int resultLimit() {
        return value(ConfigConstants.RESULT_LIMIT, ConfigConstants.RESULT_LIMIT_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int memberListCacheMaxWeight() {
        return value(ConfigConstants.MEMBER_LIST_CACHE_MAX_WEIGHT,
                ConfigConstants.MEMBER_LIST_CACHE_MAX_WEIGHT_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public int nativeTupleCacheMaxTuples() {
        return value(ConfigConstants.NATIVE_TUPLE_CACHE_MAX_TUPLES,
                ConfigConstants.NATIVE_TUPLE_CACHE_MAX_TUPLES_DEFAULT_VALUE, Integer.class);
    }

    @Override
    public String drillThroughOnAmbiguousVirtualCube() {
        return value(ConfigConstants.DRILL_THROUGH_ON_AMBIGUOUS_VIRTUAL_CUBE,
                ConfigConstants.DRILL_THROUGH_ON_AMBIGUOUS_VIRTUAL_CUBE_DEFAULT_VALUE, String.class);
    }
}
