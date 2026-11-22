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
package org.eclipse.daanse.olap.api;

import java.util.concurrent.TimeUnit;

/**
 * The tuning and behaviour settings of one {@link Context}.
 *
 * <p>
 * Every setting belongs to the context that answers for it. Two catalogs served
 * by the same server can be configured differently, and one of them changing a
 * setting cannot affect the other.
 * </p>
 *
 * <p>
 * Obtain an instance from {@link Context#getConfig()}. The keys and default
 * values behind these methods live in {@code ConfigConstants}; the same
 * descriptions are carried by the OSGi metatype of the context component, so a
 * configuration UI shows what is written here.
 * </p>
 *
 * <p>
 * <b>Each description says what the code actually does</b>, including where that
 * differs from what the name suggests, and what the default means - for most of
 * these settings the default is the only value anybody ever uses, and knowing it
 * is what tells you whether a report of odd behaviour can involve this setting
 * at all.
 * </p>
 */
public interface ContextConfig {


    // ------------------------------------------------------------------
    // Native evaluation
    //
    // "Native" means the set operation is pushed down into SQL instead of being
    // built in memory and filtered there. Native evaluation is usually far
    // faster on large dimensions, because the database does the filtering next
    // to the data. It is not always possible, and these switches exist mainly to
    // turn it off when a native path misbehaves.
    // ------------------------------------------------------------------

    /**
     * Whether Filter() may be evaluated in SQL.
     *
     * <p>
     * Default true. It is read twice: once when the catalog is built, to enable
     * the native Filter handler, and again per query, where it also decides
     * whether a Filter() nested inside a CrossJoin or TopCount argument is
     * recognised. Turning it off after the catalog is loaded therefore only
     * affects the second.
     * </p>
     */
    boolean enableNativeFilter();

    /**
     * Whether CrossJoin and NonEmptyCrossJoin may be evaluated in SQL.
     *
     * <p>
     * Default true. This is where native evaluation matters most: in memory the
     * full product is built and then filtered, in SQL only the non-empty
     * combinations are ever produced. Read when the catalog is built, so a later
     * change takes effect only after a rebuild.
     * </p>
     */
    boolean enableNativeCrossJoin();

    /**
     * Whether NON EMPTY member sets - member.children, level.members, descendants
     * - may be constrained in SQL.
     *
     * <p>
     * Default true. Beware the interaction: the evaluator derives whether native
     * evaluation is possible at all as enableNativeNonEmpty OR
     * enableNativeCrossJoin, so switching both off disables every native path
     * including TopCount and Filter, whatever their own switches say.
     * </p>
     */
    boolean enableNativeNonEmpty();

    /**
     * Whether TopCount may be evaluated in SQL.
     *
     * <p>
     * Default true. TopCount only - BottomCount never goes native, whatever this
     * is set to. Read when the catalog is built.
     * </p>
     */
    boolean enableNativeTopCount();

    /**
     * Whether non-native sub-expressions are expanded into member lists so that
     * the surrounding expression can still go native.
     *
     * <p>
     * Default false. Not a hard gate: cheap sets - LastChild, FirstChild, Lag -
     * are expanded either way. The expanded list is subject to resultLimit.
     * </p>
     */
    boolean expandNonNative();

    /**
     * Expected cardinality from which NativizeSet evaluates natively.
     *
     * <p>
     * Default 100000. Below it the fixed cost of the native path does not pay off.
     * Two places compare against this number with different estimates and
     * different operators, so a value close to a set's actual size can give
     * inconsistent decisions; change it by an order of magnitude, not by a little.
     * </p>
     */
    int nativizeMinThreshold();

    /**
     * Largest result NativizeSet will produce; 0 or less means unlimited.
     *
     * <p>
     * Default 150000. Exceeding it raises a resource-limit error rather than
     * materialising the result. Applies to NativizeSet alone - the general cap is
     * resultLimit.
     * </p>
     */
    int nativizeMaxResults();

    /**
     * What to report when a function that could have gone native has to fall back.
     *
     * <p>
     * Default OFF. WARN logs it, ERROR raises
     * NativeEvaluationUnsupportedException. Only NonEmptyCrossJoin and TopCount
     * report at all - a plain CrossJoin, Filter and native NON EMPTY never do - so
     * a quiet log is not evidence that every query went native.
     * </p>
     */
    String alertNativeEvaluationUnsupported();

    /**
     * Level cardinality below which requesting some members of a level loads all
     * of them.
     *
     * <p>
     * Default 300; 0 or less switches precaching off. Asking for two children of
     * [Store].[USA] then costs one round trip instead of one per member. The
     * comparison is strict, so a level with exactly this many members is not
     * precached.
     * </p>
     */
    int levelPreCacheThreshold();

    /**
     * Weight cap for each cached native set result store (per catalog): the sum
     * over all cached tuple lists of tuples times arity may not exceed this;
     * least-recently-used results are evicted beyond it.
     *
     * <p>
     * Default 1000000. The cache is bounded and strongly referenced - it
     * shrinks by this cap, not with memory pressure.
     * </p>
     */
    int nativeTupleCacheMaxTuples();

    // ------------------------------------------------------------------
    // Cell cache and segments
    //
    // Cell values are held in segments: rectangular blocks of the cube keyed by
    // the columns that were constrained. These settings govern how those blocks
    // are stored, shared and discarded.
    // ------------------------------------------------------------------

    /**
     * Whether cell caching is bypassed.
     *
     * <p>
     * Default false. Nothing is cleared - segments are simply not indexed, not
     * stored and not rolled up, and the member cache and the cached native tuple
     * results are switched off as well. External segment caches are never
     * attached while this is set. Useful for measuring a query from cold,
     * almost never in production.
     * </p>
     */
    boolean disableCaching();

    /**
     * Whether the in-process segment cache is used.
     *
     * <p>
     * Default false, that is, it is used. Its entire effect is that no in-memory
     * cache worker is created; the segment index, the member cache and any
     * external cache are untouched. Only sensible together with an external
     * segmentCache.
     * </p>
     */
    boolean disableLocalSegmentCache();

    /**
     * Whether every connection gets its own segment cache OVERLAY instead of
     * sharing the context's caches.
     *
     * <p>
     * Default false. It isolates one user's cached cells from another's at
     * the cost of loading the same data repeatedly. An overlay shares the
     * context's actor and executors and holds only a private in-memory
     * store; it is released when the connection closes. Independent of
     * this flag, a session with PENDING WRITEBACK changes always gets an
     * overlay - uncommitted session values must never reach the shared
     * (or external) caches.
     * </p>
     */
    boolean enableSessionCaching();

    /**
     * How many cell requests are collected before the cache manager is asked for
     * them.
     *
     * <p>
     * Default -1, which - like 0 and any negative value - means 100000. Despite
     * the name this is not an upper bound: every n-th request triggers a round to
     * the cache manager and collection then continues, so the query still
     * completes.
     * </p>
     */
    int cellBatchSize();

    /**
     * Count component of the sparse-versus-dense decision for stored cell values.
     *
     * <p>
     * Default 1000. A segment is stored sparsely when (possible - countThreshold)
     * * densityThreshold > actual. With the defaults, 1000 possible cells with
     * none filled, 2000 with 500 and 3000 with 1000 are all still dense. A
     * negative value is read as 0.
     * </p>
     */
    int sparseSegmentCountThreshold();

    /**
     * Density component of the sparse-versus-dense decision; see the count
     * threshold for the formula both values feed.
     *
     * <p>
     * Default 0.5, clamped to the range 0 to 1. At 0 segments are always stored
     * densely.
     * </p>
     */
    double sparseSegmentDensityThreshold();

    /**
     * Whether a coarser segment may be rolled up from finer segments already in
     * memory instead of querying the database again.
     *
     * <p>
     * Default true. Not sufficient on its own: the measure's aggregator has to
     * support fast aggregation, which rules out distinct counts.
     * </p>
     */
    boolean enableInMemoryRollup();

    // ------------------------------------------------------------------
    // Evaluation and MDX semantics
    //
    // These change what a query means, not just how fast it is answered.
    // Changing one of them can change reported numbers.
    // ------------------------------------------------------------------

    /**
     * How the solve order of calculated members and sets is compared.
     *
     * <p>
     * Default ABSOLUTE - solve orders are compared across the whole query, so a
     * query-defined member with solve order 1 outranks a cube-defined member with
     * 2. SCOPED resolves cube before session before query calculations and
     * compares only within one scope. An unrecognised value falls back to ABSOLUTE
     * without complaint.
     * </p>
     */
    String solveOrderMode();

    /**
     * Solve order given to the implicit calculated member that a compound slicer
     * creates.
     *
     * <p>
     * Default -99999, low enough that ordinary calculated members are evaluated
     * first. Only a compound slicer creates such a member; with a simple slicer
     * this is never read.
     * </p>
     */
    int compoundSlicerMemberSolveOrder();

    /**
     * What to do when CurrentMember is used on a hierarchy that a compound slicer
     * constrains with more than one member.
     *
     * <p>
     * Default ERROR. That combination has no well-defined answer: the slicer puts
     * a set on the hierarchy, so there is no single current member. WARN logs it,
     * OFF says nothing.
     * </p>
     */
    String currentMemberWithCompoundSlicerAlert();

    /**
     * Whether a measure is dropped from an aggregation when the tuple contains a
     * dimension the measure does not join to.
     *
     * <p>
     * Default false, and this one changes reported totals. Gender does not join to
     * [Warehouse Sales]: with false the warehouse figure at the Gender All level
     * still contributes and a report of M, F and All does not add up; with true
     * the unrelated measure is left out. Only consulted where the cube usage sets
     * IgnoreUnrelatedDimensions to false.
     * </p>
     */
    boolean ignoreMeasureForNonJoiningDimension();

    /**
     * How division handles nulls.
     *
     * <p>
     * Default false, matching SSAS: a null numerator gives null, and a non-null
     * numerator over a null denominator gives Infinity. With true both cases give
     * null. A zero denominator is not covered either way. The value is fixed into
     * the expression when it is compiled, so a change does not reach statements
     * already prepared.
     * </p>
     */
    boolean nullDenominatorProducesNull();

    /**
     * Intended to require that MDX identifiers be prefixed with their dimension,
     * so that [Gender].[M] resolves but [M] does not - failing fast instead of
     * walking every dimension, hierarchy and level of a large schema.
     *
     * <p>
     * NOT IMPLEMENTED: no code reads this key, so setting it has no effect.
     * </p>
     */
    boolean needDimensionPrefix();

    /**
     * Whether a member named in the schema but missing from the database is
     * treated as the null member while the catalog is loaded.
     *
     * <p>
     * Default false, that is, loading fails. It reaches into access control as
     * well: with true, a member grant pointing at a member that does not exist is
     * silently dropped instead of failing. During a query
     * ignoreInvalidMembersDuringQuery applies instead; the two never both matter
     * for one lookup.
     * </p>
     */
    boolean ignoreInvalidMembers();

    /**
     * Whether a member named in a query but missing from the database is treated
     * as the null member.
     *
     * <p>
     * Default false, that is, the query fails. Applies whenever the catalog is not
     * being loaded; while it is, ignoreInvalidMembers applies.
     * </p>
     */
    boolean ignoreInvalidMembersDuringQuery();

    /**
     * How many passes over the query are allowed before evaluation is given up.
     *
     * <p>
     * Default 10. A pass ends when the batched cell reader has loaded what was
     * missing; a deeply chained calculated member needs several. Exceeding the
     * limit is reported as an internal error suspecting a cycle, not as a resource
     * limit.
     * </p>
     */
    int maxEvalDepth();

    /**
     * Maximum number of iterations while aggregating over members; 0 or less means
     * no limit, and 0 is the default.
     *
     * <p>
     * Careful: the same number is also used as the recursion depth at which a
     * named set that references itself is reported - an unrelated meaning that
     * happens to share the key.
     * </p>
     */
    int iterationLimit();

    /**
     * List size from which the crossjoin optimizer is applied.
     *
     * <p>
     * Default 0, so it applies to every list - but only in a NON EMPTY context;
     * outside one the optimizer never runs, whatever the value. Setting it above
     * any realistic list size switches it off.
     * </p>
     */
    int crossJoinOptimizerSize();

    /**
     * Whether a column constraint may be dropped so that the segment loaded also
     * answers the next, slightly different question.
     *
     * <p>
     * Default true. Constraints are ranked by how much dropping them would widen
     * the result and dropped until the widening falls to a factor of two. A
     * constraint whose value list exceeded maxConstraints is dropped even with
     * this off, so the two settings interact.
     * </p>
     */
    boolean optimizePredicates();

    // ------------------------------------------------------------------
    // Aggregate tables
    //
    // Pre-aggregated tables answer coarse questions without touching the fact
    // table. useAggregates is the master switch; readAggregates only widens how
    // such tables are recognised.
    // ------------------------------------------------------------------

    /**
     * Whether aggregate tables are looked for and used.
     *
     * <p>
     * Default false. This is the master switch: with it off the catalog is never
     * scanned, so there is no aggregate table to choose. The scan happens once
     * when the catalog is built - switching this on later cannot make aggregate
     * tables appear - while the decision to use one is taken afresh for each
     * query.
     * </p>
     */
    boolean useAggregates();

    /**
     * Whether aggregate tables are also recognised by name pattern, in addition to
     * those declared explicitly in the schema.
     *
     * <p>
     * Default false. It has no effect of its own: useAggregates has to be on for
     * the database to be scanned at all.
     * </p>
     */
    boolean readAggregates();

    /**
     * How the cheapest aggregate table is picked.
     *
     * <p>
     * Default false, that is, by row count. True ranks by volume - rows times
     * columns - which is closer to the amount of data read. Not only a tiebreak:
     * an aggregate table whose chosen metric is zero is discarded, so switching
     * this can change which aggregate tables exist at all.
     * </p>
     */
    boolean chooseAggregateByVolume();

    /**
     * Whether the DDL and DML for candidate aggregate tables is written to the log
     * while aggregate requests are processed.
     *
     * <p>
     * Default false. A tool for authoring aggregate tables, not something to leave
     * on - and the generated statements go out at debug level, so the logger has
     * to be set accordingly or nothing appears.
     * </p>
     */
    boolean generateAggregateSql();

    /**
     * Whether rollups are expressed with the SQL GROUPING SETS construct, so that
     * one statement replaces several.
     *
     * <p>
     * Default false. Also requires the dialect to report support; where it does
     * not, the setting is ignored.
     * </p>
     */
    boolean enableGroupingSets();

    /**
     * Whether an XML/A drill-through response carries the total row count of the
     * underlying query.
     *
     * <p>
     * Default false, because the count costs a second query. Applies to XML/A
     * drill-through only, not to drill-through through the OLAP API.
     * </p>
     */
    boolean enableTotalCount();

    // ------------------------------------------------------------------
    // Concurrency
    // ------------------------------------------------------------------

    /**
     * How many SQL statements this context may have running at once.
     *
     * <p>
     * Default 40. A statement that finds no slot waits rather than failing. The
     * limit protects the database rather than this process: throughput usually
     * falls well before a database's own connection limit is reached.
     * </p>
     */
    int queryLimit();

    /**
     * Number of threads watching running queries for cancellation.
     *
     * <p>
     * Default 20. It sets a degree of parallelism, not a cap: the work queue
     * behind the pool is unbounded, so no request is ever refused.
     * </p>
     */
    int rolapConnectionShepherdNbThreads();

    /**
     * How often the shepherd threads look at running queries, in the unit below.
     *
     * <p>
     * Default 1000 milliseconds. This is the granularity with which a cancelled or
     * timed-out query is noticed from outside; set above queryTimeout it delays
     * that notice, though the evaluator's own checks still stop the query.
     * </p>
     */
    long rolapConnectionShepherdThreadPollingInterval();

    /**
     * Unit of the shepherd polling interval.
     *
     * <p>
     * Default MILLISECONDS.
     * </p>
     */
    TimeUnit rolapConnectionShepherdThreadPollingIntervalUnit();

    /**
     * Number of threads running SQL to populate segments.
     *
     * <p>
     * Default 100; 0 or less means unbounded. A degree of parallelism, not a cap -
     * the queue behind the pool is unbounded, so no request is refused. What
     * limits statements in flight is queryLimit.
     * </p>
     */
    int segmentCacheManagerNumberSqlThreads();

    /**
     * Number of threads talking to the external segment cache.
     *
     * <p>
     * Default 100; 0 or less means unbounded. Only relevant with an external
     * segmentCache configured.
     * </p>
     */
    int segmentCacheManagerNumberCacheThreads();

    // ------------------------------------------------------------------
    // Execution, cancellation and limits
    // ------------------------------------------------------------------

    /**
     * Limit in seconds on how long one statement may run; 0 means no limit.
     *
     * <p>
     * Default 20. The budget becomes an absolute deadline when the statement
     * starts; the evaluator checks it every checkCancelOrTimeoutInterval
     * iterations and the shepherd threads poll it, so a query that overruns fails
     * with a timeout rather than running on.
     * </p>
     */
    int queryTimeout();

    /**
     * Budget for one execution in the unit below; 0 means no limit, which is the
     * default.
     *
     * <p>
     * Unlike queryTimeout it covers the whole execution including the work after
     * the last statement returns. Enforced the same way, at the same check points.
     * </p>
     */
    long executeDuration();

    /**
     * Unit of the execution budget.
     *
     * <p>
     * Default MILLISECONDS. Every TimeUnit is honoured; an earlier form understood
     * only milliseconds and seconds and quietly read anything else as
     * milliseconds.
     * </p>
     */
    TimeUnit executeDurationUnit();

    /**
     * How many loop iterations pass between checks whether the query has been
     * cancelled.
     *
     * <p>
     * Default 1000; 0 or less switches the check off entirely rather than checking
     * every iteration. Too small costs measurable time on large result sets, too
     * large leaves a cancelled query holding its resources after the user gave up.
     * </p>
     */
    int checkCancelOrTimeoutInterval();

    /**
     * Whether drill-through is permitted.
     *
     * <p>
     * Default true. With false any attempt is refused and cells report that they
     * cannot be drilled through - a way to keep row-level data out of reach where
     * only aggregates should be visible.
     * </p>
     */
    boolean enableDrillThrough();

    // ------------------------------------------------------------------
    // Memory monitor
    // ------------------------------------------------------------------

    /**
     * Whether each execution installs a heap monitor that aborts the query instead
     * of letting it exhaust memory.
     *
     * <p>
     * Default false. It does not govern all memory-driven behaviour: caches are
     * shed under memory pressure by a separate monitor that ignores this setting.
     * </p>
     */
    boolean memoryMonitor();

    /**
     * Heap usage percentage above which the memory monitor aborts the query.
     *
     * <p>
     * Default 90. Measured after garbage collection, so it reflects live data
     * rather than garbage awaiting collection. Only consulted while memoryMonitor
     * is on.
     * </p>
     */
    int memoryMonitorThreshold();

    // ------------------------------------------------------------------
    // SQL and diagnostics
    // ------------------------------------------------------------------

    /**
     * Whether generated SQL is rendered over several lines instead of compactly.
     *
     * <p>
     * Default false. Layout only - no clause, join or predicate changes - so it is
     * safe to turn on while reading query logs, at the cost of larger logs.
     * </p>
     */
    boolean generateFormattedSql();

    /**
     * Whether a comparison test without an expected SQL statement for the current
     * dialect is reported.
     *
     * <p>
     * Default NONE, that is, silence; ANY warns for every dialect and a dialect
     * name warns only for that one. Such a test is skipped either way, which is
     * why the warning exists - a skipped test looks exactly like a passing one.
     * Read only by the legacy test harness; it has no effect on a running server.
     * </p>
     */
    String warnIfNoPatternForDialect();

    /**
     * How hard to check that expressions really are independent of the dimensions
     * they claim not to depend on.
     *
     * <p>
     * Default 0, off. A positive value makes the evaluator compute each expression
     * several times in varying contexts and compare, and the number itself seeds
     * those variations. A diagnostic for developing operators, far too slow
     * otherwise; a configured profiler takes precedence over it.
     * </p>
     */
    int testExpDependencies();

    /**
     * Whether the statements of the context are profiled: {@code off} (default), {@code on}, or {@code sample} for
     * every {@link #profileSampleEvery()}-th statement. A cube's tag {@code daanse:profile.mode} and a connection's
     * policy ({@code Context.getProfilingControl()}) override it; EXPLAIN always profiles.
     */
    String profileMode();

    /** For {@code profileMode=sample}: every how-many-th statement is profiled. Default 100. */
    int profileSampleEvery();

    /**
     * Where the plans of a profiled statement go: {@code log} (default; INFO on {@code daanse.profile}) or
     * {@code handler} (the sink of {@code Context.getProfilingControl()}).
     */
    String profileSink();

    /**
     * How much of a plan is handed on: {@code full} (default, the whole tree of calcs) or {@code top} (the
     * {@link #profileTopN()} calcs that took the most time of their own). A cube's tag {@code daanse:profile.detail}
     * overrides it.
     */
    String profileDetail();

    /** For {@code profileDetail=top}: how many calcs. Default 20; a cube's tag {@code daanse:profile.top}. */
    int profileTopN();

    // ------------------------------------------------------------------
    // Naming, ordering and result shape
    //
    // A mixed group: these settings have little to do with one another beyond
    // affecting how names are matched, how members are ordered, and how large a
    // result may become.
    // ------------------------------------------------------------------

    /**
     * Whether names are matched case-sensitively.
     *
     * <p>
     * Default false, so [gender].[f] finds [Gender].[F]. It reaches further than
     * identifier lookup: cube lookup, the Properties() function and cell property
     * lookup use it too, and with false a member looked up by name gets a
     * case-folding wrapper in the generated SQL, which can keep an index from
     * being used.
     * </p>
     */
    boolean caseSensitive();

    /**
     * Whether the InStr and InStrRev functions match case-sensitively.
     *
     * <p>
     * Default false. It affects those two functions and nothing else - not InStrB,
     * not Like, not the comparison operators - and is independent of
     * caseSensitive, which governs name lookup.
     * </p>
     */
    boolean caseSensitiveMdxInstr();

    /**
     * Whether sibling members are compared by the value of their ordinal
     * expression rather than by the order the database returned them in.
     *
     * <p>
     * Default false, that is, the SQL ORDER BY decides. It also determines whether
     * order keys are read from the database at all, so it has to be set before
     * members are loaded to take effect.
     * </p>
     */
    boolean compareSiblingsByOrderKey();

    /**
     * Whether the set argument of the Aggregate family and the two-argument form
     * of Rank are cached across evaluations.
     *
     * <p>
     * Default true. Without it, Rank([Product].CurrentMember,
     * Order([Product].MEMBERS, [Measures].[Unit Sales])) sorts the whole product
     * dimension again for every single member. Narrower than a general expression
     * cache: the Cache() function and the three-argument Rank are unaffected, and
     * the decision is taken when the statement is compiled.
     * </p>
     */
    boolean enableExpCache();

    /**
     * Whether every query axis is implicitly NON EMPTY.
     *
     * <p>
     * Default false. With true, members with no data disappear from every axis
     * without the query saying so; the slicer is not affected. It can only add NON
     * EMPTY, never remove it from an axis that asked for it.
     * </p>
     */
    boolean enableNonEmptyOnAllAxis();

    /**
     * Whether cube-specific views of shared members are cached.
     *
     * <p>
     * Default true. It has to be false for the member-level operations of
     * CacheControl, which refuse to run while the cache is on because they could
     * not keep it consistent. The value is captured when a hierarchy is built, so
     * a change takes effect only after the catalog is rebuilt.
     * </p>
     */
    boolean enableRolapCubeMemberCache();

    /**
     * Weight cap for each hierarchy's member-list caches (children lists and
     * level-member lists; the weight of an entry is its member count, and the
     * named-children store counts entries against the same cap). Least-recently-
     * used lists are evicted beyond it.
     *
     * <p>
     * Default 200000 per cache per hierarchy. The member-key cache that
     * guarantees one object per member is not governed by this - it stays
     * softly referenced.
     * </p>
     */
    int memberListCacheMaxWeight();

    /**
     * Whether snowflake dimensions are joined so that members without children are
     * filtered out.
     *
     * <p>
     * Default true. With false those queries are cheaper, but a row in an outer
     * snowflake table that no inner row references shows up as a member with no
     * children - and aggregate segment queries fall back to the older SQL
     * generator. The better fix is to remove such rows during ETL and only then
     * set this to false.
     * </p>
     */
    boolean filterChildlessSnowflakeMembers();

    /**
     * Largest number of values placed in a generated SQL IN list.
     *
     * <p>
     * Default 1000, which is a property of the database: Oracle refuses more than
     * 1000 expressions in a list, DB2 manages about 2500, most others 10000 and
     * more. Above the limit the reaction differs by site - aggregating over a
     * longer list raises an error, a native evaluation declines, and a cell
     * request silently drops the constraint and reads more rows than were asked
     * for.
     * </p>
     */
    int maxConstraints();

    /**
     * How a null member appears in results.
     *
     * <p>
     * Default #null; Analysis Services 2000 showed an empty string and 2005 showed
     * (null), so pick whichever a client expects. Not display-only: the same text
     * is compared, ignoring case, against column values to recognise a null member
     * on the way in. Set to something that occurs in the data, it makes those rows
     * generate IS NULL predicates.
     * </p>
     */
    String nullMemberRepresentation();

    /**
     * Largest permitted intermediate result; 0 or less means no limit, which is
     * the default.
     *
     * <p>
     * A brake against a single query exhausting the heap, not a correctness
     * setting - a query that trips it fails rather than returning less. One number
     * bounds several different things in different units: crossjoin tuples, the
     * capacity of a tuple list, rows read from SQL for members and for segments,
     * and the product of the member counts of all axes.
     * </p>
     */
    int resultLimit();
}
