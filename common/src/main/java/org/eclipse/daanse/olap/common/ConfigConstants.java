/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.common;

/**
 * The configuration keys of an OLAP context and the value each one takes when it
 * is not configured.
 *
 * <p>
 * This is the one place either is written down. {@code ContextConfig} names the
 * same keys as typed getters and is what production code should read;
 * {@code ContextConfigOCD} turns them into an OSGi metatype so they can be
 * described and edited from outside. Both refer back to the constants here, so a
 * default changed here changes everywhere.
 * </p>
 *
 * <p>
 * Every default is a compile-time constant on purpose - the metatype needs them
 * as constant expressions in annotations. Keep them primitive or {@code String};
 * a boxed type or an enum here breaks the metatype build.
 * </p>
 */
public class ConfigConstants {
    public static final String QUERY_LIMIT = "queryLimit";
    public static final String ENABLE_TOTAL_COUNT = "enableTotalCount";
    public static final String SEGMENT_CACHE_MANAGER_NUMBER_CACHE_THREADS = "segmentCacheManagerNumberCacheThreads";
    public static final String CELL_BATCH_SIZE = "cellBatchSize";
    public static final String ROLAP_CONNECTION_SHEPHERD_NB_THREADS = "rolapConnectionShepherdNbThreads";
    public static final String ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL = "rolapConnectionShepherdThreadPollingInterval";
    public static final String ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT = "rolapConnectionShepherdThreadPollingIntervalUnit";
    public static final String SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS = "segmentCacheManagerNumberSqlThreads";
    public static final String SOLVE_ORDER_MODE = "solveOrderMode";
    public static final String CHOOSE_AGGREGATE_BY_VOLUME = "chooseAggregateByVolume";
    public static final String DISABLE_CACHING = "disableCaching";
    public static final String DISABLE_LOCAL_SEGMENT_CACHE = "disableLocalSegmentCache";
    public static final String ENABLE_GROUPING_SETS = "enableGroupingSets";
    public static final String ENABLE_SESSION_CACHING = "enableSessionCaching";
    public static final String COMPOUND_SLICER_MEMBER_SOLVE_ORDER = "compoundSlicerMemberSolveOrder";
    public static final String ENABLE_DRILL_THROUGH = "enableDrillThrough";
    public static final String ENABLE_NATIVE_FILTER = "enableNativeFilter";
    public static final String ENABLE_NATIVE_CROSS_JOIN = "enableNativeCrossJoin";
    public static final String ENABLE_NATIVE_NON_EMPTY = "enableNativeNonEmpty";
    public static final String ENABLE_NATIVE_TOP_COUNT = "enableNativeTopCount";
    public static final String ENABLE_IN_MEMORY_ROLLUP = "enableInMemoryRollup";
    public static final String EXPAND_NON_NATIVE = "expandNonNative";
    public static final String GENERATE_AGGREGATE_SQL = "generateAggregateSql";
    public static final String IGNORE_INVALID_MEMBERS_DURING_QUERY = "ignoreInvalidMembersDuringQuery";
    public static final String IGNORE_MEASURE_FOR_NON_JOINING_DIMENSION = "ignoreMeasureForNonJoiningDimension";
    public static final String ITERATION_LIMIT = "iterationLimit";
    public static final String LEVEL_PRE_CACHE_THRESHOLD = "levelPreCacheThreshold";
    public static final String MAX_CONSTRAINTS = "maxConstraints";

    public static final String CASE_SENSITIVE = "caseSensitive";
    public static final String CASE_SENSITIVE_MDX_INSTR = "caseSensitiveMdxInstr";
    public static final String COMPARE_SIBLINGS_BY_ORDER_KEY = "compareSiblingsByOrderKey";
    public static final String ENABLE_EXP_CACHE = "enableExpCache";
    public static final String ENABLE_NON_EMPTY_ON_ALL_AXIS = "enableNonEmptyOnAllAxis";
    public static final String ENABLE_ROLAP_CUBE_MEMBER_CACHE = "enableRolapCubeMemberCache";
    public static final String FILTER_CHILDLESS_SNOWFLAKE_MEMBERS = "filterChildlessSnowflakeMembers";
    public static final String NULL_MEMBER_REPRESENTATION = "nullMemberRepresentation";
    public static final String RESULT_LIMIT = "resultLimit";
    public static final String MEMBER_LIST_CACHE_MAX_WEIGHT = "memberListCacheMaxWeight";
    public static final String NATIVE_TUPLE_CACHE_MAX_TUPLES = "nativeTupleCacheMaxTuples";
    public static final String TEST_EXP_DEPENDENCIES = "testExpDependencies";
    public static final String PROFILE_MODE = "profileMode";
    public static final String PROFILE_SAMPLE_EVERY = "profileSampleEvery";
    public static final String PROFILE_SINK = "profileSink";
    public static final String PROFILE_DETAIL = "profileDetail";
    public static final String PROFILE_TOP_N = "profileTopN";
    public static final String READ_AGGREGATES = "readAggregates";
    public static final String ALERT_NATIVE_EVALUATION_UNSUPPORTED = "alertNativeEvaluationUnsupported";
    public static final String CROSS_JOIN_OPTIMIZER_SIZE = "crossJoinOptimizerSize";
    public static final String CURRENT_MEMBER_WITH_COMPOUND_SLICER_ALERT = "currentMemberWithCompoundSlicerAlert";
    public static final String IGNORE_INVALID_MEMBERS = "ignoreInvalidMembers";
    public static final String MAX_EVAL_DEPTH = "maxEvalDepth";
    public static final String CHECK_CANCEL_OR_TIMEOUT_INTERVAL = "checkCancelOrTimeoutInterval";
    public static final String MEMORY_MONITOR = "memoryMonitor";
    public static final String WARN_IF_NO_PATTERN_FOR_DIALECT = "warnIfNoPatternForDialect";
    public static final String USE_AGGREGATES = "useAggregates";
    public static final String QUERY_TIMEOUT = "queryTimeout";
    public static final String OPTIMIZE_PREDICATES = "optimizePredicates";
    public static final String NULL_DENOMINATOR_PRODUCES_NULL = "nullDenominatorProducesNull";
    public static final String NEED_DIMENSION_PREFIX = "needDimensionPrefix";
    public static final String NATIVIZE_MIN_THRESHOLD = "nativizeMinThreshold";
    public static final String NATIVIZE_MAX_RESULTS = "nativizeMaxResults";
    public static final String SPARSE_SEGMENT_COUNT_THRESHOLD = "sparseSegmentCountThreshold";
    public static final String SPARSE_SEGMENT_DENSITY_THRESHOLD = "sparseSegmentDensityThreshold";
    public static final String MEMORY_MONITOR_THRESHOLD = "memoryMonitorThreshold";
    public static final String GENERATE_FORMATTED_SQL = "generateFormattedSql";
    public static final String EXECUTE_DURATION = "executeDuration";
    public static final String EXECUTE_DURATION_UNIT = "executeDurationUnit";

    public static final int QUERY_LIMIT_DEFAULT_VALUE = 40;
    public static final boolean ENABLE_TOTAL_COUNT_DEFAULT_VALUE = false;
    public static final int SEGMENT_CACHE_MANAGER_NUMBER_CACHE_THREADS_DEFAULT_VALUE = 100;
    public static final int CELL_BATCH_SIZE_DEFAULT_VALUE = -1;
    public static final int ROLAP_CONNECTION_SHEPHERD_NB_THREADS_DEFAULT_VALUE = 20;
    public static final long ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_DEFAULT_VALUE = 1000L;
    public static final String ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT_DEFAULT_VALUE = "MILLISECONDS";
    public static final int SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS_DEFAULT_VALUE = 100;
    public static final String SOLVE_ORDER_MODE_DEFAULT_VALUE = "ABSOLUTE";
    public static final boolean CHOOSE_AGGREGATE_BY_VOLUME_DEFAULT_VALUE = false;
    public static final boolean DISABLE_CACHING_DEFAULT_VALUE = false;
    public static final boolean DISABLE_LOCAL_SEGMENT_CACHE_DEFAULT_VALUE = false;
    public static final boolean ENABLE_GROUPING_SETS_DEFAULT_VALUE = false;
    public static final boolean ENABLE_SESSION_CACHING_DEFAULT_VALUE = false;
    public static final int COMPOUND_SLICER_MEMBER_SOLVE_ORDER_DEFAULT_VALUE = -99999;
    public static final boolean ENABLE_DRILL_THROUGH_DEFAULT_VALUE = true;
    public static final boolean ENABLE_NATIVE_FILTER_DEFAULT_VALUE = true;
    public static final boolean ENABLE_NATIVE_CROSS_JOIN_DEFAULT_VALUE = true;
    public static final boolean ENABLE_NATIVE_NON_EMPTY_DEFAULT_VALUE = true;
    public static final boolean ENABLE_NATIVE_TOP_COUNT_DEFAULT_VALUE = true;
    public static final boolean ENABLE_IN_MEMORY_ROLLUP_DEFAULT_VALUE = true;
    public static final boolean EXPAND_NON_NATIVE_DEFAULT_VALUE = false;
    public static final boolean GENERATE_AGGREGATE_SQL_DEFAULT_VALUE = false;
    public static final boolean IGNORE_INVALID_MEMBERS_DURING_QUERY_DEFAULT_VALUE = false;
    public static final boolean IGNORE_MEASURE_FOR_NON_JOINING_DIMENSION_DEFAULT_VALUE = false;
    public static final int ITERATION_LIMIT_DEFAULT_VALUE = 0;
    public static final int LEVEL_PRE_CACHE_THRESHOLD_DEFAULT_VALUE = 300;
    public static final int MAX_CONSTRAINTS_DEFAULT_VALUE = 1000;
    public static final int TEST_EXP_DEPENDENCIES_DEFAULT_VALUE = 0;
    public static final String PROFILE_MODE_DEFAULT_VALUE = "off";
    public static final int PROFILE_SAMPLE_EVERY_DEFAULT_VALUE = 100;
    public static final String PROFILE_SINK_DEFAULT_VALUE = "log";
    public static final String PROFILE_DETAIL_DEFAULT_VALUE = "full";
    public static final int PROFILE_TOP_N_DEFAULT_VALUE = 20;
    public static final boolean READ_AGGREGATES_DEFAULT_VALUE = false;
    public static final String ALERT_NATIVE_EVALUATION_UNSUPPORTED_DEFAULT_VALUE = "OFF";
    public static final int CROSS_JOIN_OPTIMIZER_SIZE_DEFAULT_VALUE = 0;
    public static final String CURRENT_MEMBER_WITH_COMPOUND_SLICER_ALERT_DEFAULT_VALUE = "ERROR";
    public static final boolean IGNORE_INVALID_MEMBERS_DEFAULT_VALUE = false;
    public static final int MAX_EVAL_DEPTH_DEFAULT_VALUE = 10;
    public static final int CHECK_CANCEL_OR_TIMEOUT_INTERVAL_DEFAULT_VALUE = 1000;
    public static final boolean MEMORY_MONITOR_DEFAULT_VALUE = false;
    public static final String WARN_IF_NO_PATTERN_FOR_DIALECT_DEFAULT_VALUE = "NONE";
    public static final boolean USE_AGGREGATES_DEFAULT_VALUE = false;
    public static final int QUERY_TIMEOUT_DEFAULT_VALUE = 20;
    public static final boolean OPTIMIZE_PREDICATES_DEFAULT_VALUE = true;
    public static final boolean NULL_DENOMINATOR_PRODUCES_NULL_DEFAULT_VALUE = false;
    public static final boolean NEED_DIMENSION_PREFIX_DEFAULT_VALUE = false;
    public static final int NATIVIZE_MIN_THRESHOLD_DEFAULT_VALUE = 100000;
    public static final int NATIVIZE_MAX_RESULTS_DEFAULT_VALUE = 150000;
    public static final int SPARSE_SEGMENT_COUNT_THRESHOLD_DEFAULT_VALUE = 1000;
    public static final double SPARSE_SEGMENT_DENSITY_THRESHOLD_DEFAULT_VALUE = 0.5;
    public static final int MEMORY_MONITOR_THRESHOLD_DEFAULT_VALUE = 90;
    public static final boolean GENERATE_FORMATTED_SQL_DEFAULT_VALUE = false;
    public static final long EXECUTE_DURATION_DEFAULT_VALUE = 0;
    public static final String EXECUTE_DURATION_UNIT_DEFAULT_VALUE = "MILLISECONDS";


    public static final boolean CASE_SENSITIVE_DEFAULT_VALUE = false;
    public static final boolean CASE_SENSITIVE_MDX_INSTR_DEFAULT_VALUE = false;
    public static final boolean COMPARE_SIBLINGS_BY_ORDER_KEY_DEFAULT_VALUE = false;
    public static final boolean ENABLE_EXP_CACHE_DEFAULT_VALUE = true;
    public static final boolean ENABLE_NON_EMPTY_ON_ALL_AXIS_DEFAULT_VALUE = false;
    public static final boolean ENABLE_ROLAP_CUBE_MEMBER_CACHE_DEFAULT_VALUE = true;
    public static final boolean FILTER_CHILDLESS_SNOWFLAKE_MEMBERS_DEFAULT_VALUE = true;
    public static final String NULL_MEMBER_REPRESENTATION_DEFAULT_VALUE = "#null";
    /** 0 means no limit. */
    public static final int RESULT_LIMIT_DEFAULT_VALUE = 0;
    /** Weight unit: one cached member in one cached list. */
    public static final int MEMBER_LIST_CACHE_MAX_WEIGHT_DEFAULT_VALUE = 200_000;
    /** Weight unit: one member of one cached native tuple. */
    public static final int NATIVE_TUPLE_CACHE_MAX_TUPLES_DEFAULT_VALUE = 1_000_000;

}
