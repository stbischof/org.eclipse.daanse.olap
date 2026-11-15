 /*
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.util;


/**
 * Holder for constants which indicate whether particular issues have been
 * fixed. Reference one of those constants in your code, and it is clear which
 * code can be enabled when the bug is fixed. Generally a constant is removed
 * when its bug is fixed.
 *
 * Cleanup items
 *
 * The following is a list of cleanup items. They are not bugs per se:
 * functionality is not wrong, just the organization of the code. If they were
 * bugs, they would be in jira. It makes sense to have the list here, so that
 * referenced class, method and variable names show up as uses in code searches.
 *
 *
 *
 * Obsolete Segment
 * Replace it by org.eclipse.daanse.olap.api.olap4j.mdx.IdentifierSegment. Likewise
 * org.eclipse.daanse.olap.api.Quoting with org.olap4j.mdx.Quoting.
 * Should wait until after the mondrian 4 'big bang', because there are ~300
 * uses of Segment in the code.
 *
 *
 *
 * @author jhyde
 * @since Oct 11, 2006
 */
public class Bug {
    /**
     * Whether Mondrian is 100% compatible with Microsoft Analysis Services
     * 2005. We know that it is not, so this constant is {@code false}.
     *
     * Use this
     * field to flag test cases whose behavior is intentionally different from
     * SSAS. If the behavior is <em>un</em>intentionally different and something
     * we want to fix, log a bug, add a new {@code BugMondrianXxxFixed} constant
     * to this class, and make the test case conditional on that constant
     * instead.
     *
     * See also the property
     * which allows the user to choose certain behaviors which are compatible
     * with SSAS 2005 but incompatible with Mondrian's previous behavior.
     */
    public static final boolean Ssas2005Compatible = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-207">MONDRIAN-207,
     * "IS EMPTY and IS NULL"</a> is fixed.
     */
    public static final boolean Bug207Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-446">bug MONDRIAN-446,
     * "Make Native NonEmpty consistant with MSAS"</a>
     * is fixed.
     */
    public static final boolean Bug446Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-328">bug MONDRIAN-328,
     * "CrossJoin no empty optimizer eliminates calculated member"</a>
     * is fixed.
     */
    public static final boolean Bug328Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-361">bug MONDRIAN-361,
     * "Aggregate Tables not working with Shared Dimensions"</a>
     * is fixed.
     *
     */
    public static final boolean Bug361Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-441">bug MONDRIAN-441,
     * "Parent-child hierarchies: &lt;Join&gt; used in dimension"</a>
     * is fixed.
     */
    public static final boolean Bug441Fixed = false;

    /**
     * Whether bug <a href="http://jira.pentaho.com/browse/MONDRIAN-495">
     * MONDRIAN-495, "Table filter concept does not support dialects."</a>
     * is fixed.
     */
    public static final boolean Bug495Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-555">bug MONDRIAN-555,
     * "Compound slicer counts cells twice in certain cases"</a> is fixed.
     * If a set in the slicer contains the same member more than once, or more
     * generally, if the regions overlap, then mondrian counts the overlaps
     * twice, whereas SSAS 2005 does not.
     */
    public static final boolean Bug555Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-584">bug MONDRIAN-584,
     * "Native evaluation returns enumerated members in the wrong order"</a>
     * is fixed. A query that includes { Gender.M, Gender.F } should return
     * results where the Gender.M values are returned before the Gender.F
     * values.
     */
    public static final boolean Bug584Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-641">bug MONDRIAN-641,
     * "Large NON EMPTY result performs poorly with ResultStyle.ITERABLE"</a>
     */
    public static final boolean Bug641Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-648">bug MONDRIAN-648,
     * "AS operator has lower precedence than required by MDX specification"</a>
     * is fixed.
     */
    public static final boolean Bug648Fixed = false;

    /**
     * Whether bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-747">bug MONDRIAN-747,
     * "When joining a shared dimension into a cube at a level
     * other than its leaf level, Mondrian gives wrong results"</a> is fixed.
     */
    public static final boolean Bug747Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-785">bug MONDRIAN-785,
     * "Native evaluation does not respect ordering"</a> is fixed.
     */
    public static final boolean Bug785Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1001">bug MONDRIAN-1001,
     * "Tests disabled due to property trigger issues"</a> is fixed.
     */
    public static final boolean Bug1001Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-2452">bug MONDRIAN-1001,
     * "Tests disabled due to property trigger issues"</a> is fixed.
     */
    public static final boolean Bug2452Fixed = false;

    /**
     * Whether
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-2440">bug MONDRIAN-1001,
     * "Error when using aggregation tables with a hierarchy that has a visible
     * level behind restricted"</a> is fixed.
     */
    public static final boolean Bug2440Fixed = false;
    /**
     * Whether RolapCubeMember and RolapMember have been fully segregated; any
     * piece of code should be working with one or the other, not both.
     */
    public static final boolean BugSegregateRolapCubeMemberFixed = false;

}
