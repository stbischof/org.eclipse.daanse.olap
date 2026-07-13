/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2021 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - removements- use plain jdk8++ java
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
package org.eclipse.daanse.olap.common;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.agg.Segment;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.calc.profile.ProfileHandler;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KeyMember;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.execution.QueryTiming;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.IdentifierSegment;
import org.eclipse.daanse.olap.api.query.KeyIdentifierSegment;
import org.eclipse.daanse.olap.api.query.NameIdentifierSegment;
import org.eclipse.daanse.olap.api.query.Quoting;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.profile.SimpleCalculationProfileWriter;
import org.eclipse.daanse.olap.exceptions.MdxCantFindMemberException;
import org.eclipse.daanse.olap.exceptions.MdxChildObjectNotFoundException;
import org.eclipse.daanse.olap.exceptions.MemberNotFoundException;
import org.eclipse.daanse.olap.exceptions.ResourceLimitExceededException;
import org.eclipse.daanse.olap.execution.ExecutionCatalogReaderWrapper;
import org.eclipse.daanse.olap.execution.ExecutionImpl;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.sort.Sorter;
import org.eclipse.daanse.olap.function.def.member.validmeasure.ValidMeasureFunDef;
import org.eclipse.daanse.olap.impl.IdentifierNode;
import org.eclipse.daanse.olap.impl.IdentifierParser;
import org.eclipse.daanse.olap.impl.KeySegmentImpl;
import org.eclipse.daanse.olap.impl.NameSegmentImpl;
import org.eclipse.daanse.olap.query.component.DimensionExpressionImpl;
import org.eclipse.daanse.olap.query.component.HierarchyExpressionImpl;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.eclipse.daanse.olap.query.component.LevelExpressionImpl;
import org.eclipse.daanse.olap.query.component.MemberExpressionImpl;
import org.eclipse.daanse.olap.query.component.NamedSetExpressionImpl;
import org.eclipse.daanse.olap.query.component.QueryPrintWriter;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;
import org.eclipse.daanse.olap.util.ArraySortedSet;
import org.eclipse.daanse.olap.util.ConcatenableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions used throughout mondrian. All methods are static.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public class Util {

    public static final String NL = System.getProperty("line.separator");

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private final static String executionStatementCleanupException =
            "An exception was encountered while trying to cleanup an execution context. A statement failed to cancel gracefully. ExecutionContext was : \"{0}\".";

    public static final Logger PROFILE_LOGGER =
            LoggerFactory.getLogger("daanse.profile");

    /**
     * Special value represents a null key.
 */
    public static final Comparable<?> sqlNullValue =
        Util.UtilComparable.INSTANCE;

    /**
     * A comparator singleton instance which can handle the presence of
     * RolapUtilComparable instances in a collection.
 */
    public static final Comparator OLAP_COMPARATOR =
        new UtilComparator();

    private static final class UtilComparator<T extends Comparable<T>>
        implements Comparator<T>
    {
        @Override
		public int compare(T o1, T o2) {
            try {
                return o1.compareTo(o2);
            } catch (ClassCastException cce) {
                if (o2 == Util.sqlNullValue) {
                    return 1;
                }
                throw new OlapRuntimeException(cce);
            }
        }
    }

    /**
     * Returns whether a cell value corresponds to the MDX NULL value, which
     * is Java {@code null}.
 */
    public static boolean isNull(Object o) {
        return o == null;
    }

    /**
     * Parses a string and returns a SHA-256 checksum of it.
     *
     * @param value The source string to parse.
     * @return A checksum of the source string.
 */
    public static byte[] digestSha256(String value) {
        final MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new UtilException(e);
        }
        return algorithm.digest(value.getBytes());
    }

    /**
     * Creates an SHA-512 hash of a String.
     *
     * @param value String to create one way hash upon.
     * @return SHA-512 hash.
 */
    public static byte[] digestSHA(final String value) {
        final MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new UtilException(e);
        }
        return algorithm.digest(value.getBytes());
    }

    /**
     * Creates an {@link ExecutorService} object backed by a thread pool.
     * @param maximumPoolSize Maximum number of concurrent
     * threads.
     * @param corePoolSize Minimum number of concurrent
     * threads to maintain in the pool, even if they are
     * idle.
     * @param keepAliveTime Time, in seconds, for which to
     * keep alive unused threads.
     * @param name The name of the threads.
     * @param rejectionPolicy The rejection policy to enforce.
     * @return An executor service preconfigured.
 */
    public static ExecutorService getExecutorService(
        int maximumPoolSize,
        int corePoolSize,
        long keepAliveTime,
        final String name,
        RejectedExecutionHandler rejectionPolicy)
    {
        // We must create a factory where the threads
        // have the right name and are marked as daemon threads.
        final ThreadFactory factory =
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
				public Thread newThread(Runnable r) {
                    final Thread t =
                        Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    t.setName(name + '_' + counter.incrementAndGet());
                    return t;
                }
            };

        // Ok, create the executor
        final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize > 0
                    ? maximumPoolSize
                    : Integer.MAX_VALUE,
                keepAliveTime,
                TimeUnit.SECONDS,
                // we use a sync queue. any other type of queue
                // will prevent the tasks from running concurrently
                // because the executors API requires blocking queues.
                // Important to pass true here. This makes the
                // order of tasks deterministic.
                // TODO Write a non-blocking queue which implements
                // the blocking queue API so we can pass that to the
                // executor.
                new LinkedBlockingQueue<>(),
                factory);

        // Set the rejection policy if required.
        if (rejectionPolicy != null) {
            executor.setRejectedExecutionHandler(
                rejectionPolicy);
        }

        // Done
        return executor;
    }

    /**
     * Creates an {@link ScheduledExecutorService} object backed by a
     * thread pool with a fixed number of threads..
     * @param maxNbThreads Maximum number of concurrent
     * threads.
     * @param name The name of the threads.
     * @return An scheduled executor service preconfigured.
 */
    public static ScheduledExecutorService getScheduledExecutorService(
        final int maxNbThreads,
        final String name)
    {
        return Executors.newScheduledThreadPool(
            maxNbThreads,
            new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger(0);
                @Override
				public Thread newThread(Runnable r) {
                    final Thread thread =
                        Executors.defaultThreadFactory().newThread(r);
                    thread.setDaemon(true);
                    thread.setName(name + '_' + counter.incrementAndGet());
                    return thread;
                }
            }
        );
    }

    /**
     * Converts a string into a double-quoted string.
 */
    public static String quoteForMdx(String val) {
        StringBuilder buf = new StringBuilder(val.length() + 20);
        quoteForMdx(buf, val);
        return buf.toString();
    }

    /**
     * Appends a double-quoted string to a string builder.
 */
    public static StringBuilder quoteForMdx(StringBuilder buf, String val) {
        buf.append("\"");
        String s0 = val.replace("\"", "\"\"");
        buf.append(s0);
        buf.append("\"");
        return buf;
    }

    /**
     * Return string quoted in [...].  For example, "San Francisco" becomes
     * "[San Francisco]"; "a [bracketed] string" becomes
     * "[a [bracketed]] string]".
 */
    public static String quoteMdxIdentifier(String id) {
    	if (id != null) {
    		StringBuilder buf = new StringBuilder(id.length() + 20);
    		quoteMdxIdentifier(id, buf);
    		return buf.toString();
    	}
    	return id;
    }

    public static void quoteMdxIdentifier(String id, StringBuilder buf) {
        buf.append('[');
        buf.append(id.replace("]", "]]"));
        buf.append(']');
    }

    /**
     * Return identifiers quoted in [...].[...].  For example, {"Store", "USA",
     * "California"} becomes "[Store].[USA].[California]".
 */
    public static String quoteMdxIdentifier(List<Segment> ids) {
        StringBuilder sb = new StringBuilder(64);
        quoteMdxIdentifier(ids, sb);
        return sb.toString();
    }

    public static void quoteMdxIdentifier(
        List<Segment> ids,
        StringBuilder sb)
    {
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            ids.get(i).toString(sb);
        }
    }

    /**
     * Quotes a string literal for Java or JavaScript.
     *
     * @param s Unquoted literal
     * @return Quoted string literal
 */
    @SuppressWarnings("java:S5361") // need use replaceAll
    public static String quoteJavaString(String s) {
        return s == null
            ? "null"
            : new StringBuilder("\"")
            .append(s.replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\\\"", "\\\\\""))
            .append("\"").toString();
    }

    /**
     * Returns whether two names are equal.
     * Takes into account the
     * {@link SystemWideProperties#CaseSensitive case sensitive option}.
     * Names may be null.
 */
    public static boolean equalName(String s, String t) {
        if (s == null) {
            return t == null;
        }
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        return caseSensitive ? s.equals(t) : s.equalsIgnoreCase(t);
    }

    /**
     * Tests two strings for equality, optionally ignoring case.
     *
     * @param s First string
     * @param t Second string
     * @param matchCase Whether to perform case-sensitive match
     * @return Whether strings are equal
 */
    public static boolean equalWithMatchCaseOption(String s, String t, boolean matchCase) {
        if (s == null) {
            return t == null;
        }
        return matchCase ? s.equals(t) : s.equalsIgnoreCase(t);
    }

    /**
     * Compares two names.  if case sensitive flag is false,
     * apply finer grain difference with case sensitive
     * Takes into account the {@link SystemWideProperties#CaseSensitive case
     * sensitive option}.
     * Names must not be null.
 */
    public static int caseSensitiveCompareName(String s, String t) {
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        if (caseSensitive) {
            return s.compareTo(t);
        } else {
            int v = s.compareToIgnoreCase(t);
            // if ignore case returns 0 compare in a case sensitive manner
            // this was introduced to solve an issue with Member.equals()
            // and Member.compareTo() not agreeing with each other
            return v == 0 ? s.compareTo(t) : v;
        }
    }

    /**
     * Compares two names.
     * Takes into account the {@link SystemWideProperties#CaseSensitive case
     * sensitive option}.
     * Names must not be null.
 */
    public static int compareName(String s, String t) {
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        return caseSensitive ? s.compareTo(t) : s.compareToIgnoreCase(t);
    }

    /**
     * Generates a normalized form of a name, for use as a key into a map.
     * Returns the upper case name if
     * {@link SystemWideProperties#CaseSensitive} is true, the name unchanged
     * otherwise.
 */
    public static String normalizeName(String s) {
        return SystemWideProperties.instance().CaseSensitive
            ? s
            : s.toUpperCase();
    }

    /**
     * Returns the result of ((Comparable) k1).compareTo(k2), with
     *
     * @see Comparable#compareTo
 */
    public static int compareKey(Object k1, Object k2) {
        return ((Comparable) k1).compareTo(k2);
    }

    /**
     * Parses an MDX identifier such as [Foo].[Bar].Baz.KeyKey2
     * and returns the result as a list of segments.
     *
     * @param s MDX identifier
     * @return List of segments
 */
    public static List<Segment> parseIdentifier(String s)  {
        return convert(
            IdentifierParser.parseIdentifier(s));
    }

    /**
     * Converts an array of name parts {"part1", "part2"} into a single string
     * "[part1].[part2]". If the names contain "]" they are escaped as "]]".
 */
    public static String implode(List<Segment> names) {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(".");
            }
            // FIXME: should be:
            //   names.get(i).toString(sb);
            // but that causes some tests to fail
            Segment segment = names.get(i);
            if (org.eclipse.daanse.olap.api.query.Quoting.UNQUOTED.equals(segment.getQuoting())) {
                segment = new IdImpl.NameSegmentImpl(((org.eclipse.daanse.olap.api.query.NameSegment) segment).getName());
            }
            segment.toString(sb);
        }
        return sb.toString();
    }

    public static String makeFqName(String name) {
        return quoteMdxIdentifier(name);
    }

    public static String makeFqName(OlapElement parent, String name) {
        if (parent == null) {
            return Util.quoteMdxIdentifier(name);
        } else {
            StringBuilder buf = new StringBuilder(64);
            buf.append(parent.getUniqueName());
            buf.append('.');
            Util.quoteMdxIdentifier(name, buf);
            return buf.toString();
        }
    }

    public static String makeFqName(String parentUniqueName, String name) {
        if (parentUniqueName == null) {
            return quoteMdxIdentifier(name);
        } else {
            StringBuilder buf = new StringBuilder(64);
            buf.append(parentUniqueName);
            buf.append('.');
            Util.quoteMdxIdentifier(name, buf);
            return buf.toString();
        }
    }

    public static OlapElement lookupCompound(
        CatalogReader schemaReader,
        OlapElement parent,
        List<Segment> names,
        boolean failIfNotFound,
        DataType category)
    {
        return lookupCompound(
            schemaReader, parent, names, failIfNotFound, category,
            MatchType.EXACT);
    }

    /**
     * Resolves a name such as
     * '[Products]&#46;[Product Department]&#46;[Produce]' by resolving the
     * components ('Products', and so forth) one at a time.
     *
     * @param catalogReader Catalog reader, supplies access-control context
     * @param parent Parent element to search in
     * @param names Exploded compound name, such as {"Products",
     *   "Product Department", "Produce"}
     * @param failIfNotFound If the element is not found, determines whether
     *   to return null or throw an error
     * @param category Type of returned element, a {@link DataType} value;
     *   {@link DataType#UNKNOWN} if it doesn't matter.
     *
     * parent != null
     * !(failIfNotFound and return == null)
     *
     * @see #parseIdentifier(String)
 */
    public static OlapElement lookupCompound(
        CatalogReader catalogReader,
        OlapElement parent,
        List<Segment> names,
        boolean failIfNotFound,
        DataType category,
        MatchType matchType)
    {
        Util.assertPrecondition(parent != null, "parent != null");

        if (LOGGER.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder(64);
            buf.append("Util.lookupCompound: ");
            buf.append("parent.name=");
            buf.append(parent.getName());
            buf.append(", category=");
            buf.append(category.getName());
            buf.append(", names=");
            quoteMdxIdentifier(names, buf);
            LOGGER.debug(buf.toString());
        }

        // First look up a member from the cache of calculated members
        // (cubes and queries both have them).
        if (category == DataType.MEMBER || category == DataType.UNKNOWN) {
            Member member = catalogReader.getCalculatedMember(names);
            if (member != null) {
                return member;
            }
        }
        // Likewise named set.
        if (category == DataType.SET || category == DataType.UNKNOWN) {
            NamedSet namedSet = catalogReader.getNamedSet(names);
            if (namedSet != null) {
                return namedSet;
            }
        }

        // Now resolve the name one part at a time.
        for (int i = 0; i < names.size(); i++) {
            OlapElement child;
            org.eclipse.daanse.olap.api.query.NameSegment name;
            if (names.get(i) instanceof org.eclipse.daanse.olap.api.query.NameSegment nameSegment) {
                name = nameSegment;
                child = catalogReader.getElementChild(parent, name, matchType);
            } else if (parent instanceof Level
                       && names.get(i) instanceof IdImpl.KeySegment
                       && names.get(i).getKeyParts().size() == 1)
            {
                // The following code is for SsasCompatibleNaming=false.
                // Continues the very limited support for key segments in
                // mondrian-3.x. To be removed in mondrian-4, when
                // SsasCompatibleNaming=true is the only option.
                final IdImpl.KeySegment keySegment = (IdImpl.KeySegment) names.get(i);
                name = keySegment.getKeyParts().get(0);
                final List<Member> levelMembers =
                    catalogReader.getLevelMembers(
                        (Level) parent, false);
                child = null;
                for (Member member : levelMembers) {
                    if (((KeyMember) member).getKey().toString().equals(
                            name.getName()))
                    {
                        child = member;
                        break;
                    }
                }
            } else {
                name = null;
                child = catalogReader.getElementChild(parent, name, matchType);
            }
            // if we're doing a non-exact match, and we find a non-exact
            // match, then for an after match, return the first child
            // of each subsequent level; for a before match, return the
            // last child
            if (child instanceof Member bestChild
                && !matchType.isExact()
                && !Util.equalName(child.getName(), name.getName()))
            {
                for (int j = i + 1; j < names.size(); j++) {
                    List<Member> childrenList =
                        catalogReader.getMemberChildren(bestChild);
                    Sorter.hierarchizeMemberList(childrenList, false);
                    if (matchType == MatchType.AFTER) {
                        bestChild = childrenList.get(0);
                    } else {
                        bestChild =
                            childrenList.get(childrenList.size() - 1);
                    }
                    if (bestChild == null) {
                        child = null;
                        break;
                    }
                }
                parent = bestChild;
                break;
            }
            if (child == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                        "Util.lookupCompound: parent.name={} has no child with name={}",
                        parent.getName(), name);
                }

                if (!failIfNotFound) {
                    return null;
                } else if (category == DataType.MEMBER) {
                    throw new MemberNotFoundException(
                        quoteMdxIdentifier(names));
                } else {
                    throw new MdxChildObjectNotFoundException(name.toString(), parent.getQualifiedName());
                }
            }
            parent = child;
            if (matchType == MatchType.EXACT_SCHEMA) {
                matchType = MatchType.EXACT;
            }
        }
        if (LOGGER.isDebugEnabled() && parent != null) {
            LOGGER.debug(
                "Util.lookupCompound: found child.name={}, child.class={}",
                parent.getName(), parent.getClass().getName());
        }

        switch (category) {
        case DIMENSION:
            if (parent instanceof Dimension) {
                return parent;
            } else if (parent instanceof Hierarchy) {
                return parent.getDimension();
            } else if (failIfNotFound) {
                throw Util.newError(
                    new StringBuilder("Can not find dimension '").append(implode(names)).append("'").toString());
            } else {
                return null;
            }
        case HIERARCHY:
            if (parent instanceof Hierarchy) {
                return parent;
            } else if (parent instanceof Dimension) {
                return parent.getHierarchy();
            } else if (failIfNotFound) {
                throw Util.newError(
                    new StringBuilder("Can not find hierarchy '").append(implode(names)).append("'").toString());
            } else {
                return null;
            }
        case LEVEL:
            if (parent instanceof Level) {
                return parent;
            } else if (failIfNotFound) {
                throw Util.newError(
                    new StringBuilder("Can not find level '").append(implode(names)).append("'").toString());
            } else {
                return null;
            }
        case MEMBER:
            if (parent instanceof Member) {
                return parent;
            } else if (failIfNotFound) {
                throw new MdxCantFindMemberException(implode(names));
            } else {
                return null;
            }
        case UNKNOWN:
            assertPostcondition(parent != null, "return != null");
            return parent;
        default:
            throw newInternal("Bad switch " + category);
        }
    }

    public static OlapElement lookup(Query q, List<Segment> nameParts) {
        final Expression exp = lookup(q, nameParts, false);
        if (exp instanceof MemberExpression memberExpr) {
            return memberExpr.getMember();
        } else if (exp instanceof LevelExpression levelExpr) {
            return levelExpr.getLevel();
        } else if (exp instanceof HierarchyExpressionImpl hierarchyExpr) {
            return hierarchyExpr.getHierarchy();
        } else if (exp instanceof DimensionExpression dimensionExpr) {
            return dimensionExpr.getDimension();
        } else {
            throw Util.newInternal("Not an olap element: " + exp);
        }
    }

    /**
     * Converts an identifier into an expression by resolving its parts into
     * an OLAP object (dimension, hierarchy, level or member) within the
     * context of a query.
     *
     * If allowProp is true, also allows property references
     * from valid members, for example
     * [Measures].[Unit Sales].FORMATTED_VALUE.
     * In this case, the result will be a {@link org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl}.
     *
     * @param q Query expression belongs to
     * @param nameParts Parts of the identifier
     * @param allowProp Whether to allow property references
     * @return OLAP object or property reference
 */
    public static Expression lookup(
        Query q,
        List<Segment> nameParts,
        boolean allowProp)
    {
        return lookup(q, q.getCatalogReader(true), nameParts, allowProp);
    }

    /**
     * Converts an identifier into an expression by resolving its parts into
     * an OLAP object (dimension, hierarchy, level or member) within the
     * context of a query.
     *
     * If allowProp is true, also allows property references
     * from valid members, for example
     * [Measures].[Unit Sales].FORMATTED_VALUE.
     * In this case, the result will be a {@link ResolvedFunCallImpl}.
     *
     * @param q Query expression belongs to
     * @param catalogReader Catalog reader
     * @param segments Parts of the identifier
     * @param allowProp Whether to allow property references
     * @return OLAP object or property reference
 */
    public static Expression lookup(
        Query q,
        CatalogReader catalogReader,
        List<Segment> segments,
        boolean allowProp)
    {
        // First, look for a calculated member defined in the query.
        final String fullName = quoteMdxIdentifier(segments);
        final CatalogReader schemaReaderSansAc =
            catalogReader.withoutAccessControl().withLocus();
        final Cube cube = q.getCube();
        // Check level properties before Member.
        // Otherwise it will query all level members to find member with property name.
        if (allowProp && segments.size() > 1) {
            List<Segment> segmentsButOne =
                    segments.subList(0, segments.size() - 1);
            final Segment lastSegment = last(segments);
            final String propertyName =
                    lastSegment instanceof org.eclipse.daanse.olap.api.query.NameSegment nameSegment
                            ? nameSegment.getName()
                            : null;
            final Member member =
                    (Member) schemaReaderSansAc.lookupCompound(
                            cube, segmentsButOne, false, DataType.MEMBER);
            if (member != null
                    && propertyName != null
                    && isValidProperty(propertyName, member.getLevel()))
            {
                return new UnresolvedFunCallImpl( new PlainPropertyOperationAtom(propertyName)
                        , new Expression[] {
                        createExpr(member)});
            }
            final Level level =
                    (Level) schemaReaderSansAc.lookupCompound(
                            cube, segmentsButOne, false, DataType.LEVEL);
            if (level != null
                    && propertyName != null
                    && isValidProperty(propertyName, level))
            {
                return new UnresolvedFunCallImpl(
                		new PlainPropertyOperationAtom(propertyName), new Expression[] {
                        createExpr(level)});
            }
        }
        // Look for any kind of object (member, level, hierarchy,
        // dimension) in the cube. Use a schema reader without restrictions.
        OlapElement olapElement =
                schemaReaderSansAc.lookupCompound(
                        cube, segments, false, DataType.UNKNOWN);

        if(olapElement == null) {
            // if we're in the middle of loading the schema, the property has
            // been set to ignore invalid members, and the member is
            // non-existent, return the null member corresponding to the
            // hierarchy of the element we're looking for; locate the
            // hierarchy by incrementally truncating the name of the element
            if (q.ignoreInvalidMembers()) {
                int nameLen = segments.size() - 1;
                olapElement = null;
                while (nameLen > 0 && olapElement == null) {
                    List<Segment> partialName =
                            segments.subList(0, nameLen);
                    olapElement = schemaReaderSansAc.lookupCompound(
                            cube, partialName, false, DataType.UNKNOWN);
                    nameLen--;
                }
                if (olapElement != null) {
                    olapElement = olapElement.getHierarchy().getNullMember();
                } else {
                    throw new MdxChildObjectNotFoundException(fullName, cube.getQualifiedName());
                }
            } else {
                throw new MdxChildObjectNotFoundException(fullName, cube.getQualifiedName());
            }
        }

        Role role = catalogReader.getRole();
        if (!role.canAccess(olapElement)) {
            throw new MdxChildObjectNotFoundException(fullName, cube.getQualifiedName());
        }
        if (olapElement instanceof Member member) {
            olapElement =
                    catalogReader.substitute(member);
        }

        // keep track of any measure members referenced; these will be used
        // later to determine if cross joins on virtual cubes can be
        // processed natively
        q.addMeasuresMembers(olapElement);
        return createExpr(olapElement);
    }

    /**
     * Looks up a cube in a schema reader.
     *
     * @param cubeName Cube name
     * @param fail Whether to fail if not found.
     * @return Cube, or null if not found
 */
    public static Cube lookupCube(
        CatalogReader schemaReader,
        String cubeName,
        boolean fail)
    {
        for (Cube cube : schemaReader.getCubes()) {
            if (Util.compareName(cube.getName(), cubeName) == 0) {
                return cube;
            }
        }
        if (fail) {
            throw new OlapRuntimeException(MessageFormat.format("MDX cube ''{0}'' not found", cubeName));
        }
        return null;
    }

    /**
     * Converts an olap element (dimension, hierarchy, level or member) into
     * an expression representing a usage of that element in an MDX statement.
 */
    public static Expression createExpr(OlapElement element)
    {
        if (element instanceof Member member) {
            return new MemberExpressionImpl(member);
        } else if (element instanceof Level level) {
            return new LevelExpressionImpl(level);
        } else if (element instanceof Hierarchy hierarchy) {
            return new HierarchyExpressionImpl(hierarchy);
        } else if (element instanceof Dimension dimension) {
            return new DimensionExpressionImpl(dimension);
        } else if (element instanceof NamedSet namedSet) {
            return new NamedSetExpressionImpl(namedSet);
        } else {
            throw Util.newInternal("Unexpected element type: " + element);
        }
    }



    /**
     * Finds a root member of a hierarchy with a given name.
     *
     * @param hierarchy Hierarchy
     * @param memberName Name of root member
     * @return Member, or null if not found
 */
    public static Member lookupHierarchyRootMember(
        CatalogReader reader,
        Hierarchy hierarchy,
        org.eclipse.daanse.olap.api.query.NameSegment memberName,
        MatchType matchType)
    {
        // Lookup member at first level.
        //
        // Don't use access control. Suppose we cannot see the 'nation' level,
        // we still want to be able to resolve '[Customer].[USA].[CA]'.
        List<Member> rootMembers = reader.getHierarchyRootMembers(hierarchy);

        // if doing an inexact search on a non-all hierarchy, create
        // a member corresponding to the name we're searching for so
        // we can use it in a hierarchical search
        Member searchMember = null;
        if (!matchType.isExact()
            && !hierarchy.hasAll()
            && !rootMembers.isEmpty())
        {
            searchMember =
                hierarchy.createMember(
                    null,
                    rootMembers.get(0).getLevel(),
                    memberName.getName(),
                    null);
        }

        int bestMatch = -1;
        int k = -1;
        for (Member rootMember : rootMembers) {
            ++k;
            int rc;
            // when searching on the ALL hierarchy, match must be exact
            if (matchType.isExact() || hierarchy.hasAll()) {
                rc = rootMember.getName().compareToIgnoreCase(memberName.getName());
            } else {
                rc = FunUtil.compareSiblingMembers(
                    rootMember,
                    searchMember);
            }
            if (rc == 0) {
                return rootMember;
            }
            if (!hierarchy.hasAll()) {
                if (matchType == MatchType.BEFORE) {
                    if (rc < 0
                        && (bestMatch == -1
                            || FunUtil.compareSiblingMembers(
                                rootMember,
                                rootMembers.get(bestMatch)) > 0))
                    {
                        bestMatch = k;
                    }
                } else if (matchType == MatchType.AFTER &&
                     (rc > 0
                         && (bestMatch == -1
                            || FunUtil.compareSiblingMembers(
                                rootMember,
                                rootMembers.get(bestMatch)) < 0))) {
                        bestMatch = k;

                }
            }
        }

        if (matchType == MatchType.EXACT_SCHEMA) {
            return null;
        }

        if (matchType != MatchType.EXACT && bestMatch != -1) {
            return rootMembers.get(bestMatch);
        }
        // If the first level is 'all', lookup member at second level. For
        // example, they could say '[USA]' instead of '[(All
        // Customers)].[USA]'.
        return (!rootMembers.isEmpty() && rootMembers.get(0).isAll())
            ? reader.lookupMemberChildByName(
                rootMembers.get(0),
                memberName,
                matchType)
            : null;
    }

    /**
     * Finds a named level in this hierarchy. Returns null if there is no
     * such level.
 */
    public static Level lookupHierarchyLevel(Hierarchy hierarchy, String s) {
        final List<? extends Level> levels = hierarchy.getLevels();
        for (Level level : levels) {
            if (level.getName().equalsIgnoreCase(s)) {
                return level;
            }
        }
        return null;
    }



    /**
     * Finds the zero based ordinal of a Member among its siblings.
 */
    public static int getMemberOrdinalInParent(
        CatalogReader reader,
        Member member)
    {
        Member parent = member.getParentMember();
        List<Member> siblings =
            (parent == null)
            ? reader.getHierarchyRootMembers(member.getHierarchy())
            : reader.getMemberChildren(parent);

        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).equals(member)) {
                return i;
            }
        }
        throw Util.newInternal(
            new StringBuilder("could not find member ").append(member).append(" amongst its siblings").toString());
    }

    /**
     * returns the first descendant on the level underneath parent.
     * If parent = [Time].[1997] and level = [Time].[Month], then
     * the member [Time].[1997].[Q1].[1] will be returned
 */
    public static Member getFirstDescendantOnLevel(
        CatalogReader reader,
        Member parent,
        Level level)
    {
        Member m = parent;
        while (m.getLevel() != level) {
            List<Member> children = reader.getMemberChildren(m);
            m = children.get(0);
        }
        return m;
    }

    /**
     * Returns whether a string is null or empty.
 */
    public static boolean isEmpty(String s) {
        return (s == null) || (s.isEmpty());
    }

    /**
     * Encloses a value in single-quotes, to make a SQL string value. Examples:
     * singleQuoteForSql(null) yields NULL;
     * singleQuoteForSql("don't") yields 'don''t'.
 */
    public static String singleQuoteString(String val) {
        StringBuilder buf = new StringBuilder(64);
        singleQuoteString(val, buf);
        return buf.toString();
    }

    /**
     * Encloses a value in single-quotes, to make a SQL string value. Examples:
     * singleQuoteForSql(null) yields NULL;
     * singleQuoteForSql("don't") yields 'don''t'.
 */
    public static void singleQuoteString(String val, StringBuilder buf) {
        buf.append('\'');

        String s0 = val.replace("'", "''");
        buf.append(s0);

        buf.append('\'');
    }


    /**
     * Returns whether a property is valid for a member of a given level.
     * It is valid if the property is defined at the level or at
     * an ancestor level, or if the property is a standard property such as
     * "FORMATTED_VALUE".
     *
     * @param propertyName Property name
     * @param level Level
     * @return Whether property is valid
 */
    public static boolean isValidProperty(
        String propertyName,
        Level level)
    {
        return lookupProperty(level, propertyName) != null;
    }

    /**
     * Finds a member property called propertyName at, or above,
     * level.
 */
    public static Property lookupProperty(
        Level level,
        String propertyName)
    {
        do {
            Property[] properties = level.getProperties();
            for (Property property : properties) {
                if (property.getName().equals(propertyName)) {
                    return property;
                }
            }
            level = level.getParentLevel();
        } while (level != null);
        // Now try a standard property.
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        final Property property = StandardProperty.lookup(propertyName, caseSensitive);
        if (property != null
            && property.isMemberProperty()
        )
        {
            return property;
        }
        return null;
    }

    public static List<Member> addLevelCalculatedMembers(
        CatalogReader reader,
        Level level,
        List<Member> members)
    {
        List<Member> calcMembers =
            reader.getCalculatedMembers(level.getHierarchy());
        List<Member> calcMembersInThisLevel = new ArrayList<>();
        for (Member calcMember : calcMembers) {
            if (calcMember.getLevel().equals(level)) {
                calcMembersInThisLevel.add(calcMember);
            }
        }
        if (!calcMembersInThisLevel.isEmpty()) {
            List<Member> newMemberList =
                new ConcatenableList<>();
            newMemberList.addAll(members);
            newMemberList.addAll(calcMembersInThisLevel);
            return newMemberList;
        }
        return members;
    }

    /**
     * Returns an exception which indicates that a particular piece of
     * functionality should work, but a developer has not implemented it yet.
 */
    public static RuntimeException needToImplement(Object o) {
        throw new UnsupportedOperationException("need to implement " + o);
    }

    /**
     * Returns an exception indicating that we didn't expect to find this value
     * here.
 */
    public static <T extends Enum<T>> RuntimeException badValue(
        Enum<T> anEnum)
    {
        return Util.newInternal(
            new StringBuilder("Was not expecting value '").append(anEnum)
                .append("' for enumeration '").append(anEnum.getDeclaringClass().getName())
                .append("' in this context").toString());
    }


    /**
     * Closes and cancels a {@link Statement} using the correct methods
     * available on the current Java runtime.
     * If errors are encountered while canceling a statement,
     * the message is logged in {@link Util}.
     * @param stmt The statement to cancel.
 */
    public static void cancelStatement(Statement stmt) {
        try {
            // A call to statement.isClosed() would be great here, but in
            // reality, some drivers will block on this check and the
            // cancellation will never happen.  This is due to the
            // non-thread-safe nature of JDBC and driver implementations. If a
            // thread is currently using the statement, calls to isClosed() are
            // synchronized internally and won't return until the query
            // completes.
            stmt.cancel();
        } catch (Throwable t) {
            // We crush this one. A lot of drivers will complain if cancel() is
            // called on a closed statement, but a call to isClosed() isn't
            // thread safe and might block. See above.

            // Also, we MUST catch all throwables. Some drivers (ie. Hive)
            // will choke on canceled queries and throw a OutOfMemoryError.
            // We can't protect ourselves against this. That's a bug on their
            // side.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("",
                    new OlapRuntimeException(MessageFormat.format(
                        executionStatementCleanupException,
                            t.getMessage()), t),
                    t);
            }
        }

    }

    /**
     * Converts a list of a string.
     *
     * For example,
     * commaList("foo", Arrays.asList({"a", "b"}))
     * returns "foo(a, b)".
     *
     * @param s Prefix
     * @param list List
     * @return String representation of string
 */
    public static <T> String commaList(
        String s,
        List<T> list)
    {
        final StringBuilder buf = new StringBuilder(s);
        buf.append("(")
            .append(list.stream()
                .map(String::valueOf)
            .collect(Collectors.joining(", ")))
            .append(")");
        return buf.toString();
    }


    /**
     * Returns whether a collection contains precisely one distinct element.
     * Returns false if the collection is empty, or if it contains elements
     * that are not the same as each other.
     *
     * @param collection Collection
     * @return boolean true if all values are same
 */
    public static <T> boolean areOccurencesEqual(
        Collection<T> collection)
    {
        Iterator<T> it = collection.iterator();
        if (!it.hasNext()) {
            // Collection is empty
            return false;
        }
        T first = it.next();
        while (it.hasNext()) {
            T t = it.next();
            if (!t.equals(first)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Parses a locale string.
     *
     * The inverse operation of {@link java.util.Locale#toString()}.
     *
     * @param localeString Locale string, e.g. "en" or "en_US"
     * @return Java locale object
 */
    public static Locale parseLocale(String localeString) {
        String[] strings = localeString.split("_");
        switch (strings.length) {
        case 1:
            return new Locale(strings[0]);
        case 2:
            return new Locale(strings[0], strings[1]);
        case 3:
            return new Locale(strings[0], strings[1], strings[2]);
        default:
            throw newInternal(
                new StringBuilder("bad locale string '").append(localeString).append("'").toString());
        }
    }

    /**
     * Converts a list of olap4j-style segments to a list of mondrian-style
     * segments.
     *
     * @param olap4jSegmentList List of olap4j segments
     * @return List of mondrian segments
 */
    public static List<Segment> convert(
        List<IdentifierSegment> olap4jSegmentList)
    {
        return olap4jSegmentList.stream().map(Util::convert).toList();
    }

    /**
     * Converts an olap4j-style segment to a mondrian-style segment.
     *
     * @param olap4jSegment olap4j segment
     * @return mondrian segment
 */
    public static Segment convert(IdentifierSegment olap4jSegment) {
        if (olap4jSegment instanceof NameIdentifierSegment nameSegment) {
            return convertNS(nameSegment);
        } else {
            return convertKs((KeySegmentImpl) olap4jSegment);
        }
    }

    private static IdImpl.KeySegment convertKs(final KeyIdentifierSegment keySegment) {
        return new IdImpl.KeySegment(
            new AbstractList<org.eclipse.daanse.olap.api.query.NameSegment>() {
                @Override
				public org.eclipse.daanse.olap.api.query.NameSegment get(int index) {
                    return convertNS(keySegment.getKeyParts().get(index));
                }

                @Override
				public int size() {
                    return keySegment.getKeyParts().size();
                }
            });
    }

    private static org.eclipse.daanse.olap.api.query.NameSegment convertNS(NameIdentifierSegment nameSegment) {
        return new IdImpl.NameSegmentImpl(
            nameSegment.getName(),
            nameSegment.getQuoting());
    }


    public static List<IdentifierSegment> toOlap4j(
        final List<Segment> segments)
    {
        return new AbstractList<>() {
            @Override
			public IdentifierSegment get(int index) {
                return toOlap4j(segments.get(index));
            }

            @Override
			public int size() {
                return segments.size();
            }
        };
    }

    public static IdentifierSegment toOlap4j(Segment segment) {
        if (org.eclipse.daanse.olap.api.query.Quoting.KEY.equals(segment.getQuoting())) {
            return toOlap4j((IdImpl.KeySegment) segment);
        } else {
            return toOlap4j((org.eclipse.daanse.olap.api.query.NameSegment) segment);
        }
    }

    private static KeySegmentImpl toOlap4j(final IdImpl.KeySegment keySegment) {
        return new KeySegmentImpl(
            new AbstractList<NameSegmentImpl>() {
                @Override
				public NameSegmentImpl get(int index) {
                    return toOlap4j(keySegment.subSegmentList.get(index));
                }

                @Override
				public int size() {
                    return keySegment.subSegmentList.size();
                }
            });
    }

    private static NameSegmentImpl toOlap4j(org.eclipse.daanse.olap.api.query.NameSegment nameSegment) {
        return new NameSegmentImpl(
            null,
            nameSegment.getName(),
            toOlap4j(nameSegment.getQuoting()));
    }

    public static Quoting toOlap4j(org.eclipse.daanse.olap.api.query.Quoting quoting) {
        return Quoting.valueOf(quoting.name());
    }

    // TODO: move to IdentifierSegment
    public static boolean matches(IdentifierSegment segment, String name) {
        switch (segment.getQuoting()) {
        case KEY:
            return false; // FIXME
        case QUOTED:
            return equalName(segment.getName(), name);
        case UNQUOTED:
            return segment.getName().equalsIgnoreCase(name);
        default:
            throw unexpected(segment.getQuoting());
        }
    }

    public static boolean matches(
        Member member, List<Segment> nameParts)
    {
        if (Util.equalName(Util.implode(nameParts),
            member.getUniqueName()))
        {
            // exact match
            return true;
        }
        Segment segment = nameParts.get(nameParts.size() - 1);
        while (member.getParentMember() != null) {
            if (!segment.matches(member.getName())) {
                return false;
            }
            member = member.getParentMember();
            nameParts = nameParts.subList(0, nameParts.size() - 1);
            segment = nameParts.get(nameParts.size() - 1);
        }
        if (segment.matches(member.getName())) {
            return Util.equalName(
                member.getHierarchy().getUniqueName(),
                Util.implode(nameParts.subList(0, nameParts.size() - 1)));
        } else if (member.isAll()) {
            return Util.equalName(
                member.getHierarchy().getUniqueName(),
                Util.implode(nameParts));
        } else {
            return false;
        }
    }


    public static RuntimeException newElementNotFoundException(
    		DataType category,
        IdentifierNode identifierNode)
    {
        String type;
        switch (category) {
        case MEMBER:
            return new MemberNotFoundException(
                identifierNode.toString());
        case UNKNOWN:
            type = "Element";
            break;
        default:
            type = category.getName();
        }
        return newError(new StringBuilder(type).append(" '").append(identifierNode).append("' not found").toString());
    }

    /**
     * Calls {@link java.util.concurrent.Future#get()} and converts any
     * throwable into a non-checked exception.
     *
     * @param future Future
     * @param message Message to qualify wrapped exception
     * @param <T> Result type
     * @return Result
 */
    public static <T> T safeGet(Future<T> future, String message) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newError(e, message);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (cause instanceof Error error) {
                throw error;
            } else {
                throw newError(cause, message);
            }
        }
    }


    /**
     * As Arrays#binarySearch(Object[], int, int, Object), but
     * available pre-JDK 1.6.
 */
    public static <T extends Comparable<T>> int binarySearch(
        T[] ts, int start, int end, T t)
    {
        return Arrays.binarySearch(
                ts, start, end, t,
                Util.OLAP_COMPARATOR);
    }

    /**
     * Returns the intersection of two sorted sets. Does not modify either set.
     *
     * Optimized for the case that both sets are {@link ArraySortedSet}.
     *
     * @param set1 First set
     * @param set2 Second set
     * @return Intersection of the sets
 */
    public static <E extends Comparable> SortedSet<E> intersect(
        SortedSet<E> set1,
        SortedSet<E> set2)
    {
        if (set1.isEmpty()) {
            return set1;
        }
        if (set2.isEmpty()) {
            return set2;
        }
        if (!(set1 instanceof ArraySortedSet)
            || !(set2 instanceof ArraySortedSet))
        {
            final TreeSet<E> set = new TreeSet<>(set1);
            set.retainAll(set2);
            return set;
        }
        final Comparable<?>[] result =
            new Comparable[Math.min(set1.size(), set2.size())];
        final Iterator<E> it1 = set1.iterator();
        final Iterator<E> it2 = set2.iterator();
        int i = 0;
        E e1 = it1.next();
        E e2 = it2.next();
        for (;;) {
            final int compare = e1.compareTo(e2);
            if (compare == 0) {
                result[i++] = e1;
                if (!it1.hasNext() || !it2.hasNext()) {
                    break;
                }
                e1 = it1.next();
                e2 = it2.next();
            } else if (compare > 0) {
                if (!it2.hasNext()) {
                    break;
                }
                e2 = it2.next();
            } else {
                if (!it1.hasNext()) {
                    break;
                }
                e1 = it1.next();
            }
        }
        return new ArraySortedSet(result, 0, i);
    }


    /**
     * Returns the last item in a list.
     *
     * @param list List
     * @param <T> Element type
     * @return Last item in the list
     * @throws IndexOutOfBoundsException if list is empty
 */
    public static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }


    /**
     * Closes a JDBC result set, statement, and connection, ignoring any errors.
     * If any of them are null, that's fine.
     *
     * If any of them throws a {@link SQLException}, returns the first
     * such exception, but always executes all closes.
     *
     * @param resultSet Result set
     * @param statement Statement
     * @param connection Connection
 */
    public static SQLException close(
        ResultSet resultSet,
        Statement statement,
        Connection connection)
    {
        SQLException firstException = null;
        if (resultSet != null) {
            try {
                if (statement == null) {
                    statement = resultSet.getStatement();
                }
                resultSet.close();
            } catch (Exception t) {
                firstException = new SQLException();
                firstException.initCause(t);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        return firstException;
    }

    public static SQLException close(
        CellSet resultSet,
        org.eclipse.daanse.olap.impl.StatementImpl statement,
        org.eclipse.daanse.olap.api.connection.Connection connection)
    {
        SQLException firstException = null;
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception t) {
                firstException = new SQLException();
                firstException.initCause(t);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        return firstException;
    }

    /**
     * Creates a bitset with bits from {@code fromIndex} (inclusive) to
     * specified {@code toIndex} (exclusive) set to {@code true}.
     *
     * For example, {@code bitSetBetween(0, 3)} returns a bit set with bits
     * {0, 1, 2} set.
     *
     * @param fromIndex Index of the first bit to be set.
     * @param toIndex   Index after the last bit to be set.
     * @return Bit set
 */
    public static BitSet bitSetBetween(int fromIndex, int toIndex) {
        final BitSet bitSet = new BitSet();
        if (toIndex > fromIndex) {
            // Avoid http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6222207
            // "BitSet internal invariants may be violated"
            bitSet.set(fromIndex, toIndex);
        }
        return bitSet;
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T[] genericArray(Class<T> clazz, int size) {
        return (T[]) Array.newInstance(clazz, size);
    }

    /**
     * Throws an internal error if condition is not true. It would be called
     * assert, but that is a keyword as of JDK 1.4.
 */
    public static void assertTrue(boolean b) {
        if (!b) {
            throw newInternal("assert failed");
        }
    }

    /**
     * Throws an internal error with the given messagee if condition is not
     * true. It would be called assert, but that is a keyword as
     * of JDK 1.4.
 */
    public static void assertTrue(boolean b, String message) {
        if (!b) {
            throw newInternal("assert failed: " + message);
        }
    }

    /**
     * Creates an internal error with a given message.
 */
    public static RuntimeException newInternal(String message) {
        return new OlapRuntimeException(MessageFormat.format("Internal error: {0}", message));
    }

    /**
     * Creates an internal error with a given message and cause.
 */
    public static RuntimeException newInternal(Throwable e, String message) {
        return new OlapRuntimeException(MessageFormat.format("Internal error: {0}", message), e);
    }

    /**
     * Creates a non-internal error. Currently implemented in terms of
     * internal errors, but later we will create resourced messages.
 */
    public static RuntimeException newError(String message) {
        return newInternal(message);
    }

    /**
     * Creates a non-internal error. Currently implemented in terms of
     * internal errors, but later we will create resourced messages.
 */
    public static RuntimeException newError(Throwable e, String message) {
        return newInternal(e, message);
    }

    /**
     * Returns an exception indicating that we didn't expect to find this value
     * here.
     *
     * @param value Value
 */
    public static RuntimeException unexpected(Enum value) {
        return Util.newInternal(
            new StringBuilder("Was not expecting value '").append(value)
                .append("' for enumeration '").append(value.getClass().getName())
                .append("' in this context").toString());
    }

    /**
     * Checks that a precondition (declared using the javadoc @pre
     * tag) is satisfied.
     *
     * @param b The value of executing the condition
 */
    public static void assertPrecondition(boolean b) {
        assertTrue(b);
    }

    /**
     * Checks that a precondition (declared using the javadoc @pre
     * tag) is satisfied. For example,
     *
     * void f(String s) {
     *    Util.assertPrecondition(s != null, "s != null");
     *    ...
     * }
     *
     * @param b The value of executing the condition
     * @param condition The text of the condition
 */
    public static void assertPrecondition(boolean b, String condition) {
        assertTrue(b, condition);
    }



    /**
     * Checks that a postcondition (declared using the javadoc
     * tag) is satisfied.
     *
     * @param b The value of executing the condition
 */
    public static void assertPostcondition(boolean b, String condition) {
        assertTrue(b, condition);
    }

    /**
     * Constructs the message associated with an arbitrary Java error, making
     * up one based on the stack trace if there is none. As
     * {@link #getErrorMessage(Throwable,boolean)}, but does not print the
     * class name if the exception is derived from {@link java.sql.SQLException}
     * or is exactly a {@link java.lang.Exception}.
 */
    public static String getErrorMessage(Throwable err) {
        boolean prependClassName =
            !(err instanceof java.sql.SQLException
              || err.getClass() == java.lang.Exception.class);
        return getErrorMessage(err, prependClassName);
    }

    /**
     * Constructs the message associated with an arbitrary Java error, making
     * up one based on the stack trace if there is none.
     *
     * @param err the error
     * @param prependClassName should the error be preceded by the
     *   class name of the Java exception?  defaults to false, unless the error
     *   is derived from {@link java.sql.SQLException} or is exactly a {@link
     *   java.lang.Exception}
 */
    public static String getErrorMessage(
        Throwable err,
        boolean prependClassName)
    {
        String errMsg = err.getMessage();
        if ((errMsg == null) || (err instanceof RuntimeException)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            err.printStackTrace(pw);
            return sw.toString();
        } else {
            return (prependClassName)
                ? new StringBuilder(err.getClass().getName()).append(": ").append(errMsg).toString()
                : errMsg;
        }
    }

    /**
     * If one of the causes of an exception is of a particular class, returns
     * that cause. Otherwise returns null.
     *
     * @param e Exception
     * @param clazz Desired class
     * @param <T> Class
     * @return Cause of given class, or null
 */
    public static <T extends Throwable>
    T getMatchingCause(Throwable e, Class<T> clazz) {
        for (;;) {
            if (clazz.isInstance(e)) {
                return clazz.cast(e);
            }
            final Throwable cause = e.getCause();
            if (cause == null || cause == e) {
                return null;
            }
            e = cause;
        }
    }

    /**
     * Converts an expression to a string.
 */
    public static String unparse(Expression exp) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exp.unparse(pw);
        return sw.toString();
    }

    /**
     * Converts an query to a string.
 */
    public static String unparse(Query query) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new QueryPrintWriter(sw);
        query.unparse(pw);
        return sw.toString();
    }

    /**
     * Creates a file-protocol URL for the given file.
 */
    public static URL toURL(File file) throws MalformedURLException {
        String path = file.getAbsolutePath();
        // This is a bunch of weird code that is required to
        // make a valid URL on the Windows platform, due
        // to inconsistencies in what getAbsolutePath returns.
        String fs = System.getProperty("file.separator");
        if (fs.length() == 1) {
            char sep = fs.charAt(0);
            if (sep != '/') {
                path = path.replace(sep, '/');
            }
            if (path.charAt(0) != '/') {
                path = '/' + path;
            }
        }
        path = "file://" + path;
        return new URL(path);
    }

    /**
     * Combines two integers into a hash code.
 */
    public static int hash(int i, int j) {
        return (i << 4) ^ j;
    }

    /**
     * Computes a hash code from an existing hash code and an object (which
     * may be null).
 */
    public static int hash(int h, Object o) {
        int k = (o == null) ? 0 : o.hashCode();
        return ((h << 4) | h) ^ k;
    }

    /**
     * Computes a hash code from an existing hash code and an array of objects
     * (which may be null).
 */
    public static int hashArray(int h, Object [] a) {
        // The hashcode for a null array and an empty array should be different
        // than h, so use magic numbers.
        if (a == null) {
            return hash(h, 19690429);
        }
        if (a.length == 0) {
            return hash(h, 19690721);
        }
        for (Object anA : a) {
            h = hash(h, anA);
        }
        return h;
    }

    /**
     * Concatenates one or more arrays.
     *
     * Resulting array has same element type as first array. Each arrays may
     * be empty, but must not be null.
     *
     * @param a0 First array
     * @param as Zero or more subsequent arrays
     * @return Array containing all elements
 */
    public static <T> T[] appendArrays(
        T[] a0,
        T[]... as)
    {
        int n = a0.length;
        for (T[] a : as) {
            n += a.length;
        }
        T[] copy = Arrays.copyOf(a0, n);
        n = a0.length;
        for (T[] a : as) {
            System.arraycopy(a, 0, copy, n, a.length);
            n += a.length;
        }
        return copy;
    }

    /**
     * Adds an object to the end of an array.  The resulting array is of the
     * same type (e.g. String[]) as the input array.
     *
     * @param a Array
     * @param o Element
     * @return New array containing original array plus element
     *
     * @see #appendArrays
 */
    public static <T> T[] append(T[] a, T o) {
        T[] a2 = Arrays.copyOf(a, a.length + 1);
        a2[a.length] = o;
        return a2;
    }

    /**
     * Like {@link java.util.Arrays}.copyOf(double[], int), but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
 */
    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(
            original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Like {@link java.util.Arrays}.copyOf(int[], int), but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
 */
    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(
            original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Like {@link java.util.Arrays}.copyOf(long[], int), but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
 */
    public static long[] copyOf(long[] original, int newLength) {
        long[] copy = new long[newLength];
        System.arraycopy(
            original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Creates a very simple implementation of {@link Validator}. (Only
     * useful for resolving trivial expressions.)
 */
    public static Validator createSimpleValidator(final FunctionService functionService) {
        return new Validator() {
            @Override
			public Query getQuery() {
                return null;
            }

            @Override
			public CatalogReader getCatalogReader() {
                throw new UnsupportedOperationException();
            }

            @Override
			public Expression validate(Expression exp, boolean scalar) {
                return exp;
            }

            @Override
			public void validate(ParameterExpression parameterExpr) {
                //empty
            }

            @Override
			public void validate(MemberProperty memberProperty) {
                //empty
            }

            @Override
			public void validate(QueryAxis axis) {
                //empty
            }

            @Override
			public void validate(Formula formula) {
                //empty
            }

            @Override
			public FunctionDefinition getDef(Expression[] args, OperationAtom operationAtom) {
                // Very simple resolution. Assumes that there is precisely
                // one resolver (i.e. no overloading) and no argument
                // conversions are necessary.
                List<FunctionResolver> resolvers = functionService.getResolvers(operationAtom);
                final FunctionResolver resolver = resolvers.get(0);
                final List<FunctionResolver.Conversion> conversionList =
                    new ArrayList<>();
                final FunctionDefinition def =
                    resolver.resolve(args, this, conversionList);
                assert conversionList.isEmpty();
                return def;
            }

            @Override
			public boolean alwaysResolveFunDef() {
                return false;
            }

            @Override
			public boolean canConvert(
                int ordinal, Expression fromExp,
                DataType to,
                List<FunctionResolver.Conversion> conversions)
            {
                return true;
            }

            @Override
			public boolean requiresExpression() {
                return false;
            }

            @Override
			public FunctionService getFunctionService() {
                return functionService;
            }

            @Override
			public Parameter createOrLookupParam(
                boolean definition,
                String name,
                Type type,
                Expression defaultExp,
                String description)
            {
                return null;
            }
        };
    }

    /**
     * Reads a Reader until it returns EOF and returns the contents as a String.
     *
     * @param rdr  Reader to Read.
     * @param bufferSize size of buffer to allocate for reading.
     * @return content of Reader as String
     * @throws IOException on I/O error
 */
    public static String readFully(final Reader rdr, final int bufferSize)
        throws IOException
    {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                "Buffer size must be greater than 0");
        }

        final char[] buffer = new char[bufferSize];
        final StringBuilder buf = new StringBuilder(bufferSize);

        int len;
        while ((len = rdr.read(buffer)) != -1) {
            buf.append(buffer, 0, len);
        }
        return buf.toString();
    }

    /**
     * Reads an input stream until it returns EOF and returns the contents as an
     * array of bytes.
     *
     * @param in  Input stream
     * @param bufferSize size of buffer to allocate for reading.
     * @return content of stream as an array of bytes
     * @throws IOException on I/O error
 */
    public static byte[] readFully(final InputStream in, final int bufferSize)
        throws IOException
    {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                "Buffer size must be greater than 0");
        }

        final byte[] buffer = new byte[bufferSize];
        final ByteArrayOutputStream baos =
            new ByteArrayOutputStream(bufferSize);

        int len;
        while ((len = in.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * Returns the contents of a URL, substituting tokens.
     *
     * Replaces the tokens "${key}" if the map is not null and "key" occurs
     * in the key-value map.
     *
     * If the URL string starts with "inline:" the contents are the
     * rest of the URL.
     *
     * @param urlStr  URL string
     * @param map Key/value map
     * @return Contents of URL with tokens substituted
     * @throws IOException on I/O error
 */
    public static String readURL(final String urlStr, Map<String, String> map)
        throws IOException
    {
        if (urlStr.startsWith("inline:")) {
            String content = urlStr.substring("inline:".length());
            if (map != null) {
                content = Util.replaceProperties(content, map);
            }
            return content;
        } else {
            final URL url = new URL(urlStr);
            return readURL(url, map);
        }
    }



    /**
     * Returns the contents of a URL, substituting tokens.
     *
     * Replaces the tokens "${key}" if the map is not null and "key" occurs
     * in the key-value map.
     *
     * @param url URL
     * @param map Key/value map
     * @return Contents of URL with tokens substituted
     * @throws IOException on I/O error
 */
    public static String readURL(
        final URL url,
        Map<String, String> map)
        throws IOException
    {
        final int BUF_SIZE = 8096;
        try (Reader r = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String xmlCatalog = readFully(r, BUF_SIZE);
            return Util.replaceProperties(xmlCatalog, map);
        }
    }

    /**
     * Replaces tokens in a string.
     *
     * Replaces the tokens "${key}" if "key" occurs in the key-value map.
     * Otherwise "${key}" is left in the string unchanged.
     *
     * @param text Source string
     * @param env Map of key-value pairs
     * @return String with tokens substituted
 */
    public static String replaceProperties(
        String text,
        Map<String, String> env)
    {
        // Since Java 9, Matcher.appendReplacement accepts StringBuilder
        StringBuilder buf = new StringBuilder(text.length() + 200);

        Pattern pattern = Pattern.compile("\\$\\{([^${}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = env.get(varName);
            if (varValue != null) {
                matcher.appendReplacement(buf, varValue);
            } else {
                matcher.appendReplacement(buf, "\\${$1}");
            }
        }
        matcher.appendTail(buf);

        return buf.toString();
    }

    public static String printMemory() {
        return printMemory(null);
    }

    public static String printMemory(String msg) {
        final Runtime rt = Runtime.getRuntime();
        final long freeMemory = rt.freeMemory();
        final long totalMemory = rt.totalMemory();
        final StringBuilder buf = new StringBuilder(64);

        buf.append("FREE_MEMORY:");
        if (msg != null) {
            buf.append(msg);
            buf.append(':');
        }
        buf.append(' ');
        buf.append(freeMemory / 1024);
        buf.append("kb ");

        long hundredths = (freeMemory * 10000) / totalMemory;

        buf.append(hundredths / 100);
        hundredths %= 100;
        if (hundredths >= 10) {
            buf.append('.');
        } else {
            buf.append(".0");
        }
        buf.append(hundredths);
        buf.append('%');

        return buf.toString();
    }

    /**
     * Casts a Set to a Set with a different element type.
     *
     * @param set Set
     * @return Set of desired type
 */
    @SuppressWarnings({"unchecked"})
    public static <T> Set<T> cast(Set<?> set) {
        return (Set<T>) set;
    }

    /**
     * Casts a List to a List with a different element type.
     *
     * @param list List
     * @return List of desired type
 */
    @SuppressWarnings({"unchecked"})
    public static <T> List<T> cast(List<?> list) {
        return (List<T>) list;
    }

    /**
     * Returns whether it is safe to cast a collection to a collection with a
     * given element type.
     *
     * @param collection Collection
     * @param clazz Target element type
     * @param <T> Element type
     * @return Whether all not-null elements of the collection are instances of
     *   element type
 */
    public static <T> boolean canCast(
        Collection<?> collection,
        Class<T> clazz)
    {
        for (Object o : collection) {
            if (o != null && !clazz.isInstance(o)) {
                return false;
            }
        }
        return true;
    }



    /**
     * Looks up an enumeration by name, returning null if null or not valid.
     *
     * @param clazz Enumerated type
     * @param name Name of constant
 */
    public static <E extends Enum<E>> E lookup(Class<E> clazz, String name) {
        return lookup(clazz, name, null);
    }

    /**
     * Looks up an enumeration by name, returning a given default value if null
     * or not valid.
     *
     * @param clazz Enumerated type
     * @param name Name of constant
     * @param defaultValue Default value if constant is not found
     * @return Value, or null if name is null or value does not exist
 */
    public static <E extends Enum<E>> E lookup(
        Class<E> clazz, String name, E defaultValue)
    {
        if (name == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Check the resultSize against the result limit setting. Throws
     * LimitExceededDuringCrossjoin exception if limit exceeded.
     *
     * When it is called from RolapNativeSet.checkCrossJoin(), it is only
     * possible to check the known input size, because the final CJ result
     * will come from the DB(and will be checked against the limit when
     * fetching from the JDBC result set, in SqlTupleReader.prepareTuples())
     *
     * @param resultSize Result limit
     * @throws ResourceLimitExceededException
 */
    public static void checkCJResultLimit(long resultSize) {
        int resultLimit = SystemWideProperties.instance().ResultLimit;

        // Throw an exeption, if the size of the crossjoin exceeds the result
        // limit.
        if (resultLimit > 0 && resultLimit < resultSize) {
            throw new ResourceLimitExceededException(
                resultSize, resultLimit);
        }

        // Throw an exception if the crossjoin exceeds a reasonable limit.
        // (Yes, 4 billion is a reasonable limit.)
        if (resultSize > Integer.MAX_VALUE) {
            throw new ResourceLimitExceededException(
                resultSize, Integer.MAX_VALUE);
        }
    }

    /**
     * Garbage-collecting iterator. Iterates over a collection of references,
     * and if any of the references has been garbage-collected, removes it from
     * the collection.
     *
     * @param <T> Element type
 */
    public static class GcIterator<T> implements Iterator<T> {
        private final Iterator<? extends Reference<T>> iterator;
        private boolean hasNext;
        private T next;

        public GcIterator(Iterator<? extends Reference<T>> iterator) {
            this.iterator = iterator;
            this.hasNext = true;
            moveToNext();
        }

        /**
         * Creates an iterator over a collection of references.
         *
         * @param referenceIterable Collection of references
         * @param <T2> element type
         * @return iterable over collection
 */
        public static <T2> Iterable<T2> over(
            final Iterable<? extends Reference<T2>> referenceIterable)
        {
            return new Iterable<>() {
                @Override
				public Iterator<T2> iterator() {
                    return new GcIterator<>(referenceIterable.iterator());
                }
            };
        }

        private void moveToNext() {
            while (iterator.hasNext()) {
                final Reference<T> ref = iterator.next();
                next = ref.get();
                if (next != null) {
                    return;
                }
                iterator.remove();
            }
            hasNext = false;
        }

        @Override
		public boolean hasNext() {
            return hasNext;
        }

        @Override
		public T next() {
            if(!hasNext()){
                throw new NoSuchElementException();
            }
            final T next1 = next;
            moveToNext();
            return next1;
        }

        @Override
		public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * This class implements the Knuth-Morris-Pratt algorithm
     * to search within a byte array for a token byte array.
 */
    public static class ByteMatcher {
        private final int[] matcher;
        public final byte[] key;
        public ByteMatcher(byte[] key) {
            this.key = key;
            this.matcher = compile(key);
        }
        /**
         * Matches the pre-compiled byte array token against a
         * byte array variable and returns the index of the key
         * within the array.
         * @param a An array of bytes to search for.
         * @return -1 if not found, or the index (0 based) of the match.
 */
        public int match(byte[] a) {
            int j = 0;
            for (int i = 0; i < a.length; i++) {
                while (j > 0 && key[j] != a[i]) {
                    j = matcher[j - 1];
                }
                if (a[i] == key[j]) {
                    j++;
                }
                if (key.length == j) {
                    return
                        i - key.length + 1;
                }
            }
            return -1;
        }
        private int[] compile(byte[] key) {
            int[] matcherInner = new int[key.length];
            int j = 0;
            for (int i = 1; i < key.length; i++) {
                while (j > 0 && key[j] != key[i]) {
                    j = matcherInner[j - 1];
                }
                if (key[i] == key[j]) {
                    j++;
                }
                matcherInner[i] = j;
            }
            return matcherInner;
        }
    }

    /**
     * Transforms a list into a map for which all the keys return
     * a null value associated to it.
     *
     * The list passed as an argument will be used to back
     * the map returned and as many methods are overridden as
     * possible to make sure that we don't iterate over the backing
     * list when creating it and when performing operations like
     * .size(), entrySet() and contains().
     *
     * The returned map is to be considered immutable. It will
     * throw an {@link UnsupportedOperationException} if attempts to
     * modify it are made.
 */
    public static <K, V> Map<K, V> toNullValuesMap(List<K> list) {
        return new NullValuesMap<>(list);
    }

    private static class NullValuesMap<K, V> extends AbstractMap<K, V> {
        private final List<K> list;
        private NullValuesMap(List<K> list) {
            super();
            this.list = Collections.unmodifiableList(list);
        }
        @Override
		public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
				public Iterator<Entry<K, V>>
                    iterator()
                {
                    return new Iterator<>() {
                        private int pt = 0;
                        @Override
						public void remove() {
                            throw new UnsupportedOperationException();
                        }
                        @Override
						@SuppressWarnings("unchecked")
                        public Entry<K, V> next() {
                            if(!hasNext()){
                                throw new NoSuchElementException();
                            }
                            return new AbstractMap.SimpleEntry(
                                list.get(pt++), null) {};
                        }
                        @Override
						public boolean hasNext() {
                            return pt < list.size();
                        }
                    };
                }
                @Override
				public int size() {
                    return list.size();
                }
                @Override
				public boolean contains(Object o) {
                    return  (o instanceof Entry entry && list.contains(entry.getKey()));
                }
            };
        }
        @Override
		public Set<K> keySet() {
            return new AbstractSet<>() {
                @Override
				public Iterator<K> iterator() {
                    return new Iterator<>() {
                        private int pt = -1;
                        @Override
						public void remove() {
                            throw new UnsupportedOperationException();
                        }
                        @Override
						public K next() {
                            if(!hasNext()){
                                throw new NoSuchElementException();
                            }
                            return list.get(++pt);
                        }
                        @Override
						public boolean hasNext() {
                            return pt < list.size();
                        }
                    };
                }
                @Override
				public int size() {
                    return list.size();
                }
                @Override
				public boolean contains(Object o) {
                    return list.contains(o);
                }
            };
        }
        @Override
		public Collection<V> values() {
            return new AbstractList<>() {
                @Override
				public V get(int index) {
                    return null;
                }
                @Override
				public int size() {
                    return list.size();
                }
                @Override
				public boolean contains(Object o) {
                    return (o == null && size() > 0);
                }
            };
        }
        @Override
		public V get(Object key) {
            return null;
        }
        @Override
		public boolean containsKey(Object key) {
            return list.contains(key);
        }
        @Override
		public boolean containsValue(Object o) {
            return  (o == null && size() > 0);
        }
    }

  /**
   * Called during major steps of executing a MDX query to provide insight into Calc calls/times
   * and key function calls/times.
   *
   * @param handler
   * @param title
   * @param calc
   * @param timing
 */
  public static void explain( ProfileHandler handler, String title, Calc<?> calc, QueryTiming timing ) {
    if ( handler == null ) {
      return;
    }
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter( stringWriter );

	SimpleCalculationProfileWriter spw = new SimpleCalculationProfileWriter(printWriter);

    printWriter.println( title );
    if ( calc != null ) {

    	if (calc instanceof Calc pc) {

			CalculationProfile calcProfile = pc.getCalculationProfile();
			spw.write(calcProfile);

		} else {
			printWriter.println("UNPROFILED: " + calc.getClass().getName());

		}
    }
    printWriter.close();
    handler.explain( stringWriter.toString(), timing );
  }

  /**
   * Wraps a CatalogReader so that each call has a locus for profiling purposes.
   *
   * <p>This method creates a {@link ExecutionCatalogReaderWrapper} that automatically
   * manages the execution context for every method call on the CatalogReader interface.
   *
   * @param connection Connection providing execution context
   * @param schemaReader CatalogReader to wrap
   * @return Wrapped CatalogReader with automatic execution context management
 */
  public static CatalogReader executionCatalogReader(
      org.eclipse.daanse.olap.api.connection.Connection connection,
      final CatalogReader schemaReader)
  {
      final org.eclipse.daanse.olap.api.execution.Statement statement = connection.getInternalStatement();
      final ExecutionImpl execution = new ExecutionImpl(statement,
          ExecuteDurationUtil.executeDurationValue(connection.getContext()));
      return new ExecutionCatalogReaderWrapper(execution, "Schema reader", schemaReader);
  }

    /**
     * Comparable value, equal only to itself. Used to represent the NULL value,
     * as returned from a SQL query.
 */
    private static final class UtilComparable
        implements Comparable, Serializable
    {
        private static final long serialVersionUID = -2595758291465179116L;

        public static final Util.UtilComparable INSTANCE =
            new Util.UtilComparable();

        // singleton
        private UtilComparable() {
        }

        // do not override equals and hashCode -- use identity

        @Override
        public String toString() {
            return "#null";
        }

        @Override
        public int compareTo(Object o) {
            // collates after everything (except itself)
            return o == this ? 0 : -1;
        }
    }

    public static boolean containsValidMeasure( Expression... expressions ) {
        for ( Expression expression : expressions ) {
          if ( expression instanceof ResolvedFunCall fun ) {
            return fun.getFunDef() instanceof ValidMeasureFunDef || containsValidMeasure( fun.getArgs() );
          }
        }
        return false;
      }
}
