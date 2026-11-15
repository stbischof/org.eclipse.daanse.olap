/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2004-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
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

package org.eclipse.daanse.olap.query.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.query.NameSegment;
import org.eclipse.daanse.olap.api.query.Quoting;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.common.Util.ByteMatcher;
import  org.eclipse.daanse.olap.util.ArraySortedSet;
import org.eclipse.daanse.olap.util.ArrayStack;
import org.eclipse.daanse.olap.util.ByteString;
import org.eclipse.daanse.olap.util.CartesianProductList;
import org.junit.jupiter.api.Test;

/**
 * Tests for methods in {@link org.eclipse.daanse.olap.common.Util} and, sometimes, classes in
 * the {@code mondrian.util} package.
 */
 class UtilTestCase{

    @Test
     void quoteMdxIdentifier() {
        assertThat(Util.quoteMdxIdentifier("San Francisco")).isEqualTo("[San Francisco]");
        assertThat(Util.quoteMdxIdentifier("a [bracketed] string")).isEqualTo("[a [bracketed]] string]");
        assertThat(Util.quoteMdxIdentifier(
                Arrays.<Segment>asList(
                        new IdImpl.NameSegmentImpl("Store"),
                        new IdImpl.NameSegmentImpl("USA"),
                        new IdImpl.NameSegmentImpl("California")))).isEqualTo("[Store].[USA].[California]");
    }

    @Test
     void quoteJava() {
        assertThat(Util.quoteJavaString("San Francisco")).isEqualTo("\"San Francisco\"");
        assertThat(Util.quoteJavaString("null")).isEqualTo("\"null\"");
        assertThat(Util.quoteJavaString(null)).isEqualTo("null");
        assertThat(Util.quoteJavaString("a\\b\"c")).isEqualTo("\"a\\\\b\\\"c\"");
    }

    @Test
     void bufReplace() {
        // Replace with longer string. Search pattern at beginning & end.
        checkReplace("xoxox", "x", "yy", "yyoyyoyy");

        // Replace with shorter string.
        checkReplace("xxoxxoxx", "xx", "z", "zozoz");

        // Replace with empty string.
        checkReplace("xxoxxoxx", "xx", "", "oo");

        // Replacement string contains search string. (A bad implementation
        // might loop!)
        checkReplace("xox", "x", "xx", "xxoxx");

        // Replacement string combines with characters in the original to
        // match search string.
        checkReplace("cacab", "cab", "bb", "cabb");

        // Seek string does not exist.
        checkReplace(
            "the quick brown fox", "coyote", "wolf",
            "the quick brown fox");

        // Empty buffer.
        checkReplace("", "coyote", "wolf", "");

        // Empty seek string. This is a bit mean!
        checkReplace("fox", "", "dog", "dogfdogodogxdog");
    }

    @Test
     void compareKey() {
        assertThat(Util.compareKey(Boolean.FALSE, Boolean.TRUE) < 0).isTrue();
    }

    private static void checkReplace(
        String original, String seek, String replace, String expected)
    {
        // Check whether the JDK does what we expect. (If it doesn't it's
        // probably a bug in the test, not the JDK.)
        assertThat(original.replaceAll(seek, replace)).isEqualTo(expected);

        // Check the StringBuilder version of replace.
        String modified = original.replace(seek, replace);
        assertThat(modified).isEqualTo(expected);

        // Check the String version of replace.
        assertThat(original.replace(seek, replace)).isEqualTo(expected);
    }

    @Test
     void implode() {
        List<Segment> fooBar = Arrays.<Segment>asList(
            new IdImpl.NameSegmentImpl("foo", Quoting.UNQUOTED),
            new IdImpl.NameSegmentImpl("bar", Quoting.UNQUOTED));
        assertThat(Util.implode(fooBar)).isEqualTo("[foo].[bar]");

        List<Segment> empty = Collections.emptyList();
        assertThat(Util.implode(empty)).isEqualTo("");

        List<Segment> nasty = Arrays.<Segment>asList(
            new IdImpl.NameSegmentImpl("string", Quoting.UNQUOTED),
            new IdImpl.NameSegmentImpl("with", Quoting.UNQUOTED),
            new IdImpl.NameSegmentImpl("a [bracket] in it", Quoting.UNQUOTED));
        assertThat(Util.implode(nasty)).isEqualTo("[string].[with].[a [bracket]] in it]");
    }

    @Test
     void parseIdentifier() {
        List<Segment> strings =
                Util.parseIdentifier("[string].[with].[a [bracket]] in it]");
        assertThat(strings.size()).isEqualTo(3);
        assertThat(name(strings, 2)).isEqualTo("a [bracket] in it");

        strings =
            Util.parseIdentifier("[Worklog].[All].[calendar-[LANGUAGE]].js]");
        assertThat(strings.size()).isEqualTo(3);
        assertThat(name(strings, 2)).isEqualTo("calendar-[LANGUAGE].js");

        // allow spaces before, after and between
        strings = Util.parseIdentifier("  [foo] . [bar].[baz]  ");
        assertThat(strings.size()).isEqualTo(3);
        final int index = 0;
        assertThat(name(strings, index)).isEqualTo("foo");

        // first segment not quoted
        strings = Util.parseIdentifier("Time.1997.[Q3]");
        assertThat(strings.size()).isEqualTo(3);
        assertThat(name(strings, 0)).isEqualTo("Time");
        assertThat(name(strings, 1)).isEqualTo("1997");
        assertThat(name(strings, 2)).isEqualTo("Q3");

        // spaces ignored after unquoted segment
        strings = Util.parseIdentifier("[Time . Weekly ] . 1997 . [Q3]");
        assertThat(strings.size()).isEqualTo(3);
        assertThat(name(strings, 0)).isEqualTo("Time . Weekly ");
        assertThat(name(strings, 1)).isEqualTo("1997");
        assertThat(name(strings, 2)).isEqualTo("Q3");

        // identifier ending in '.' is invalid
        try {
            strings = Util.parseIdentifier("[foo].[bar].");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Expected identifier after '.', "
                    + "in member identifier '[foo].[bar].'");
        }

        try {
            strings = Util.parseIdentifier("[foo].[bar");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Expected ']', in member identifier '[foo].[bar'");
        }

        try {
            strings = Util.parseIdentifier("[Foo].[Bar], [Baz]");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid member identifier '[Foo].[Bar], [Baz]'");
        }
    }

    private String name(List<Segment> strings, int index) {
        final Segment segment = strings.get(index);
        return ((NameSegment) segment).getName();
    }

    @Test
     void replaceProperties() {
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        map.put("empty", "");
        map.put("null", null);
        map.put("foobarbaz", "bang!");
        map.put("malformed${foo", "groovy");

        assertThat(Util.replaceProperties("a${foo}b", map)).isEqualTo("abarb");
        assertThat(Util.replaceProperties("twice${foo}${foo}", map)).isEqualTo("twicebarbar");
        assertThat(Util.replaceProperties("${foo} at start", map)).isEqualTo("bar at start");
        assertThat(Util.replaceProperties("x${empty}y${empty}${empty}z", map)).isEqualTo("xyz");
        assertThat(Util.replaceProperties("x${nonexistent}${foo}", map)).isEqualTo("x${nonexistent}bar");

        // malformed tokens are left as is
        assertThat(Util.replaceProperties("${malformed${foo}${foo}", map)).isEqualTo("${malformedbarbar");

        // string can contain '$'
        assertThat(Util.replaceProperties("x$foo", map)).isEqualTo("x$foo");

        // property with empty name is always ignored -- even if it's in the map
        assertThat(Util.replaceProperties("${}", map)).isEqualTo("${}");
        map.put("", "v");
        assertThat(Util.replaceProperties("${}", map)).isEqualTo("${}");

        // if a property's value is null, it's as if it doesn't exist
        assertThat(Util.replaceProperties("${null}", map)).isEqualTo("${null}");

        // nested properties are expanded, but not recursively
        assertThat(Util.replaceProperties("${foo${foo}baz}", map)).isEqualTo("${foobarbaz}");
    }

    @Test
     void areOccurrencesEqual() {
        assertThat(Util.areOccurencesEqual(Collections.<String>emptyList())).isFalse();
        assertThat(Util.areOccurencesEqual(Arrays.asList("x"))).isTrue();
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "x"))).isTrue();
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "y"))).isFalse();
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "y", "x"))).isFalse();
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "x", "x"))).isTrue();
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "x", "y", "z"))).isFalse();
    }

    /**
     * Unit test for {@link mondrian.util.ArrayStack}.
     */
    @Test
     void arrayStack() {
        final ArrayStack<String> stack = new ArrayStack<>();
        assertThat(stack.size()).isEqualTo(0);
        stack.add("a");
        assertThat(stack.size()).isEqualTo(1);
        assertThat(stack.peek()).isEqualTo("a");
        stack.push("b");
        assertThat(stack.size()).isEqualTo(2);
        assertThat(stack.peek()).isEqualTo("b");
        assertThat(stack.pop()).isEqualTo("b");
        assertThat(stack.size()).isEqualTo(1);
        stack.add(0, "z");
        assertThat(stack.peek()).isEqualTo("a");
        assertThat(stack.size()).isEqualTo(2);
        stack.push(null);
        assertThat(stack.size()).isEqualTo(3);
        assertThat(stack).isEqualTo(Arrays.asList("z", "a", null));
        String z = "";
        for (String s : stack) {
            z += s;
        }
        assertThat(z).isEqualTo("zanull");
        stack.clear();
        assertThatThrownBy(() -> stack.peek()).isInstanceOf(EmptyStackException.class);
        assertThatThrownBy(() -> stack.pop()).isInstanceOf(EmptyStackException.class);
    }

    /**
     * Tests {@link Util#appendArrays(Object[], Object[][])}.
     */
    @Test
     void appendArrays() {
        String[] a0 = {"a", "b", "c"};
        String[] a1 = {"foo", "bar"};
        String[] empty = {};

        final String[] strings1 = Util.appendArrays(a0, a1);
        assertThat(strings1.length).isEqualTo(5);
        assertThat(Arrays.asList(strings1)).isEqualTo(Arrays.asList("a", "b", "c", "foo", "bar"));

        final String[] strings2 = Util.appendArrays(
            empty, a0, empty, a1, empty);
        assertThat(Arrays.asList(strings2)).isEqualTo(Arrays.asList("a", "b", "c", "foo", "bar"));

        Number[] n0 = {Math.PI};
        Integer[] i0 = {123, null, 45};
        Float[] f0 = {0f};

        final Number[] numbers = Util.appendArrays(n0, i0, f0);
        assertThat(numbers.length).isEqualTo(5);
        assertThat(Arrays.asList(numbers)).isEqualTo(Arrays.asList((Number) Math.PI, 123, null, 45, 0f));
    }

    @Test
     void canCast() {
        assertThat(Util.canCast(Collections.EMPTY_LIST, Integer.class)).isTrue();
        assertThat(Util.canCast(Collections.EMPTY_LIST, String.class)).isTrue();
        assertThat(Util.canCast(Collections.EMPTY_SET, String.class)).isTrue();
        assertThat(Util.canCast(Arrays.asList(1, 2), Integer.class)).isTrue();
        assertThat(Util.canCast(Arrays.asList(1, 2), Number.class)).isTrue();
        assertThat(Util.canCast(Arrays.asList(1, 2), String.class)).isFalse();
        assertThat(Util.canCast(Arrays.asList(1, null, 2d), Number.class)).isTrue();
        assertThat(Util.canCast(
                new HashSet<Object>(Arrays.asList(1, null, 2d)),
                Number.class)).isTrue();
        assertThat(Util.canCast(Arrays.asList(1, null, 2d), Integer.class)).isFalse();
    }

    /**
     * Unit test for {@link Util#parseLocale(String)} method.
     */
    @Test
     void parseLocale() {
        Locale[] locales = {
            Locale.CANADA,
            Locale.CANADA_FRENCH,
            Locale.getDefault(),
            Locale.US,
            Locale.TRADITIONAL_CHINESE,
        };
        for (Locale locale : locales) {
            assertThat(Util.parseLocale(locale.toString())).isEqualTo(locale);
        }
        // Example locale names in Locale.toString() javadoc.
        String[] localeNames = {
            "en", "de_DE", "_GB", "en_US_WIN", "de__POSIX", "fr__MAC"
        };
        for (String localeName : localeNames) {
            assertThat(Util.parseLocale(localeName).toString()).isEqualTo(localeName);
        }
    }


    void checkMonikerValid(String moniker) {
        final String digits = "0123456789";
        final String validChars =
            "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "$_";
        assertThat(moniker.length() > 0).isTrue();
        // all chars are valid
        for (int i = 0; i < moniker.length(); i++) {
            assertThat(validChars.indexOf(moniker.charAt(i)) >= 0).as(moniker).isTrue();
        }
        // does not start with digit
        assertThat(digits.indexOf(moniker.charAt(0)) >= 0).as(moniker).isFalse();
    }



    @Test
     void cartesianProductList() {
        final CartesianProductList<String> list =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.asList("a", "b"),
                    Arrays.asList("1", "2", "3")));
        assertThat(list.size()).isEqualTo(6);
        assertThat(list.isEmpty()).isFalse();
        checkCartesianListContents(list);

        assertThat(list.toString()).isEqualTo("[[a, 1], [a, 2], [a, 3], [b, 1], [b, 2], [b, 3]]");

        // One element empty
        final CartesianProductList<String> list2 =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.<String>asList(),
                    Arrays.asList("1", "2", "3")));
        assertThat(list2.isEmpty()).isTrue();
        assertThat(list2.toString()).isEqualTo("[]");
        checkCartesianListContents(list2);

        // Other component empty
        final CartesianProductList<String> list3 =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.asList("a", "b"),
                    Arrays.<String>asList()));
        assertThat(list3.isEmpty()).isTrue();
        assertThat(list3.toString()).isEqualTo("[]");
        checkCartesianListContents(list3);

        // Zeroary
        final CartesianProductList<String> list4 =
            new CartesianProductList<>(
                Collections.<List<String>>emptyList());
        assertThat(list4.isEmpty()).isFalse();
//        assertEquals("[[]]", list4.toString());
        checkCartesianListContents(list4);

        // 1-ary
        final CartesianProductList<String> list5 =
            new CartesianProductList<>(
                Collections.singletonList(
                    Arrays.asList("a", "b")));
        assertThat(list5.toString()).isEqualTo("[[a], [b]]");
        checkCartesianListContents(list5);

        // 3-ary
        final CartesianProductList<String> list6 =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.asList("a", "b", "c", "d"),
                    Arrays.asList("1", "2"),
                    Arrays.asList("x", "y", "z")));
        assertThat(list6.size()).isEqualTo(24); // 4 * 2 * 3
        assertThat(list6.isEmpty()).isFalse();
        assertThat(list6.get(0).toString()).isEqualTo("[a, 1, x]");
        assertThat(list6.get(1).toString()).isEqualTo("[a, 1, y]");
        assertThat(list6.get(23).toString()).isEqualTo("[d, 2, z]");
        checkCartesianListContents(list6);

        final Object[] strings = new Object[6];
        list6.getIntoArray(1, strings);
        assertThat(Arrays.asList(strings).toString()).isEqualTo("[a, 1, y, null, null, null]");

        CartesianProductList<Object> list7 =
            new CartesianProductList<>(
                Arrays.<List<Object>>asList(
                    Arrays.<Object>asList(
                        "1",
                        Arrays.asList("2a", null, "2c"),
                        "3"),
                    Arrays.<Object>asList(
                        "a",
                        Arrays.asList("bb", "bbb"),
                        "c",
                        "d")));
        list7.getIntoArray(1, strings);
        assertThat(Arrays.asList(strings).toString()).isEqualTo("[1, bb, bbb, null, null, null]");
        list7.getIntoArray(5, strings);
        assertThat(Arrays.asList(strings).toString()).isEqualTo("[2a, null, 2c, bb, bbb, null]");
        checkCartesianListContents(list7);
    }

    private <T> void checkCartesianListContents(CartesianProductList<T> list) {
        List<List<T>> arrayList = new ArrayList<>();
        for (List<T> ts : list) {
            arrayList.add(ts);
        }
        assertThat(list).isEqualTo(arrayList);
    }

    /**
     * Unit test for {@link ByteString}.
     */
    @Test
     void byteString() {
        final ByteString empty0 = new ByteString(new byte[]{});
        final ByteString empty1 = new ByteString(new byte[]{});
        assertThat(empty1).isEqualTo(empty0);
        assertThat(empty1.hashCode()).isEqualTo(empty0.hashCode());
        assertThat(empty0.toString()).isEqualTo("");
        assertThat(empty0.length()).isEqualTo(0);
        assertThat(empty0.compareTo(empty0)).isEqualTo(0);
        assertThat(empty0.compareTo(empty1)).isEqualTo(0);

        final ByteString two =
            new ByteString(new byte[]{ (byte) 0xDE, (byte) 0xAD});
        assertThat(two).isNotEqualTo(empty0);
        assertThat(empty0).isNotEqualTo(two);
        assertThat(two.toString()).isEqualTo("dead");
        assertThat(two.length()).isEqualTo(2);
        assertThat(two.compareTo(two)).isEqualTo(0);
        assertThat(empty0.compareTo(two) < 0).isTrue();
        assertThat(two.compareTo(empty0) > 0).isTrue();

        final ByteString three =
            new ByteString(new byte[]{ (byte) 0xDE, (byte) 0x02, (byte) 0xAD});
        assertThat(three.length()).isEqualTo(3);
        assertThat(three.toString()).isEqualTo("de02ad");
        assertThat(two.compareTo(three) < 0).isTrue();
        assertThat(three.compareTo(two) > 0).isTrue();
        org.junit.jupiter.api.Assertions.assertEquals(0x02, three.byteAt(1));

        final HashSet<ByteString> set = new HashSet<>(Arrays.asList(empty0, two, three, two, empty1, three));
        assertThat(set.size()).isEqualTo(3);
    }

    /**
     * Unit test for {@link Util#binarySearch}.
     */
    @Test
     void binarySearch() {
        final String[] abce = {"a", "b", "c", "e"};
        assertThat(Util.binarySearch(abce, 0, 4, "a")).isEqualTo(0);
        assertThat(Util.binarySearch(abce, 0, 4, "b")).isEqualTo(1);
        assertThat(Util.binarySearch(abce, 1, 4, "b")).isEqualTo(1);
        assertThat(Util.binarySearch(abce, 0, 4, "d")).isEqualTo(-4);
        assertThat(Util.binarySearch(abce, 1, 4, "d")).isEqualTo(-4);
        assertThat(Util.binarySearch(abce, 2, 4, "d")).isEqualTo(-4);
        assertThat(Util.binarySearch(abce, 2, 3, "d")).isEqualTo(-4);
        assertThat(Util.binarySearch(abce, 2, 3, "e")).isEqualTo(-4);
        assertThat(Util.binarySearch(abce, 2, 3, "f")).isEqualTo(-4);
        assertThat(Util.binarySearch(abce, 0, 4, "f")).isEqualTo(-5);
        assertThat(Util.binarySearch(abce, 2, 4, "f")).isEqualTo(-5);
    }

    /**
     * Unit test for {@link mondrian.util.ArraySortedSet}.
     */
    @Test
     void arraySortedSet() {
        String[] abce = {"a", "b", "c", "e"};
        final SortedSet<String> abceSet =
            new ArraySortedSet<>(abce);

        // test size, isEmpty, contains
        assertThat(abceSet.size()).isEqualTo(4);
        assertThat(abceSet.isEmpty()).isFalse();
        assertThat(abceSet.first()).isEqualTo("a");
        assertThat(abceSet.last()).isEqualTo("e");
        assertThat(abceSet.contains("a")).isTrue();
        assertThat(abceSet.contains("aa")).isFalse();
        assertThat(abceSet.contains("z")).isFalse();
        assertThat(abceSet.contains(null)).isFalse();
        checkToString("[a, b, c, e]", abceSet);

        // test iterator
        String z = "";
        for (String s : abceSet) {
            z += s + ";";
        }
        assertThat(z).isEqualTo("a;b;c;e;");

        // empty set
        String[] empty = {};
        final SortedSet<String> emptySet =
            new ArraySortedSet<>(empty);
        int n = 0;
        for (String s : emptySet) {
            ++n;
        }
        assertThat(n).isEqualTo(0);
        assertThat(emptySet.size()).isEqualTo(0);
        assertThat(emptySet.isEmpty()).isTrue();
        assertThatThrownBy(() -> emptySet.first()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> emptySet.last()).isInstanceOf(NoSuchElementException.class);
        assertThat(emptySet.contains("a")).isFalse();
        assertThat(emptySet.contains("aa")).isFalse();
        assertThat(emptySet.contains("z")).isFalse();
        checkToString("[]", emptySet);

        // same hashCode etc. as similar hashset
        final HashSet<String> abcHashset = new HashSet<>(Arrays.asList(abce));
        assertThat(abceSet).isEqualTo(abcHashset);
        assertThat(abcHashset).isEqualTo(abceSet);
        assertThat(abcHashset.hashCode()).isEqualTo(abceSet.hashCode());

        // subset to end
        final Set<String> subsetEnd = new ArraySortedSet<>(abce, 1, 4);
        checkToString("[b, c, e]", subsetEnd);
        assertThat(subsetEnd.size()).isEqualTo(3);
        assertThat(subsetEnd.isEmpty()).isFalse();
        assertThat(subsetEnd.contains("c")).isTrue();
        assertThat(subsetEnd.contains("a")).isFalse();
        assertThat(subsetEnd.contains("z")).isFalse();

        // subset from start
        final Set<String> subsetStart = new ArraySortedSet<>(abce, 0, 2);
        checkToString("[a, b]", subsetStart);
        assertThat(subsetStart.size()).isEqualTo(2);
        assertThat(subsetStart.isEmpty()).isFalse();
        assertThat(subsetStart.contains("a")).isTrue();
        assertThat(subsetStart.contains("c")).isFalse();

        // subset from neither start or end
        final Set<String> subset = new ArraySortedSet<>(abce, 1, 2);
        checkToString("[b]", subset);
        assertThat(subset.size()).isEqualTo(1);
        assertThat(subset.isEmpty()).isFalse();
        assertThat(subset.contains("b")).isTrue();
        assertThat(subset.contains("a")).isFalse();
        assertThat(subset.contains("e")).isFalse();

        // empty subset
        final Set<String> subsetEmpty = new ArraySortedSet<>(abce, 1, 1);
        checkToString("[]", subsetEmpty);
        assertThat(subsetEmpty.size()).isEqualTo(0);
        assertThat(subsetEmpty.isEmpty()).isTrue();
        assertThat(subsetEmpty.contains("e")).isFalse();

        // subsets based on elements, not ordinals
        assertThat(subsetStart).isEqualTo(abceSet.subSet("a", "c"));
        assertThat(abceSet.subSet("a", "d").toString()).isEqualTo("[a, b, c]");
        assertThat(subsetStart).isNotEqualTo(abceSet.subSet("a", "e"));
        assertThat(subsetStart).isNotEqualTo(abceSet.subSet("b", "c"));
        assertThat(abceSet.subSet("c", "z").toString()).isEqualTo("[c, e]");
        assertThat(abceSet.subSet("d", "z").toString()).isEqualTo("[e]");
        assertThat(subsetStart).isNotEqualTo(abceSet.subSet("e", "c"));
        assertThat(abceSet.subSet("e", "c").toString()).isEqualTo("[]");
        assertThat(subsetStart).isNotEqualTo(abceSet.subSet("z", "c"));
        assertThat(abceSet.subSet("z", "c").toString()).isEqualTo("[]");

        // merge
        final ArraySortedSet<String> ar1 = new ArraySortedSet<>(abce);
        final ArraySortedSet<String> ar2 =
            new ArraySortedSet<>(
                new String[] {"d"});
        final ArraySortedSet<String> ar3 =
            new ArraySortedSet<>(
                new String[] {"b", "c"});
        checkToString("[a, b, c, e]", ar1);
        checkToString("[d]", ar2);
        checkToString("[b, c]", ar3);
        checkToString("[a, b, c, d, e]", ar1.merge(ar2));
        checkToString("[a, b, c, e]", ar1.merge(ar3));
    }

    private void checkToString(String expected, Set<String> set) {
        assertThat(set.toString()).isEqualTo(expected);

        final List<String> list = new ArrayList<>(set);
        assertThat(list.toString()).isEqualTo(expected);

        list.clear();
        for (String s : set) {
            list.add(s);
        }
        assertThat(list.toString()).isEqualTo(expected);
    }


     void testIntersectSortedSet() {
        final ArraySortedSet<String> ace =
            new ArraySortedSet(new String[]{ "a", "c", "e"});
        final ArraySortedSet<String> cd =
            new ArraySortedSet(new String[]{ "c", "d"});
        final ArraySortedSet<String> bdf =
            new ArraySortedSet(new String[]{ "b", "d", "f"});
        final ArraySortedSet<String> bde =
            new ArraySortedSet(new String[]{ "b", "d", "e"});
        final ArraySortedSet<String> empty =
            new ArraySortedSet(new String[]{});
        checkToString("[a, c, e]", Util.intersect(ace, ace));
        checkToString("[c]", Util.intersect(ace, cd));
        checkToString("[]", Util.intersect(ace, empty));
        checkToString("[]", Util.intersect(empty, ace));
        checkToString("[]", Util.intersect(empty, empty));
        checkToString("[]", Util.intersect(ace, bdf));
        checkToString("[e]", Util.intersect(ace, bde));
    }

    @Test
     void rolapUtilComparator() throws Exception {
        final Comparable[] compArray =
            new Comparable[] {
                "1",
                "2",
                "3",
                "4"
        };
        // Will throw a ClassCastException if it fails.
        Util.binarySearch(
            compArray, 0, compArray.length,
            (Comparable)Util.sqlNullValue);
    }

    @Test
     void byteMatcher() throws Exception {
        final ByteMatcher bm = new ByteMatcher(new byte[] {(byte)0x2A});
        final byte[] bytesNotPresent =
            new byte[] {(byte)0x2B, (byte)0x2C};
        final byte[] bytesPresent =
            new byte[] {(byte)0x2B, (byte)0x2A, (byte)0x2C};
        final byte[] bytesPresentLast =
            new byte[] {(byte)0x2B, (byte)0x2C, (byte)0x2A};
        final byte[] bytesPresentFirst =
                new byte[] {(byte)0x2A, (byte)0x2C, (byte)0x2B};
        assertThat(bm.match(bytesNotPresent)).isEqualTo(-1);
        assertThat(bm.match(bytesPresent)).isEqualTo(1);
        assertThat(bm.match(bytesPresentLast)).isEqualTo(2);
        assertThat(bm.match(bytesPresentFirst)).isEqualTo(0);
    }

    /**
     * This is a simple test to make sure that
     * {@link Util#toNullValuesMap(List)} never iterates on
     * its source list upon creation.
     */
    @Test
     void nullValuesMap() throws Exception {
        class BaconationException extends RuntimeException {};
        Map<String, Object> nullValuesMap =
            Util.toNullValuesMap(
                new ArrayList<>(
                    Arrays.asList(
                        "CHUNKY",
                        "BACON!!"))
                {
                    private static final long serialVersionUID = 1L;
                    @Override
					public String get(int index) {
                        throw new BaconationException();
                    }
                    @Override
					public Iterator<String> iterator() {
                        throw new BaconationException();
                    }
                });
        assertThatThrownBy(() -> nullValuesMap.entrySet().iterator().next()).isInstanceOf(BaconationException.class);
        // None of the above operations should trigger an iteration.
        assertThat(nullValuesMap.entrySet().isEmpty()).isFalse();
        assertThat(nullValuesMap.containsKey("CHUNKY")).isTrue();
        assertThat(nullValuesMap.containsValue(null)).isTrue();
        assertThat(nullValuesMap.containsKey(null)).isFalse();
        assertThat(nullValuesMap.keySet().contains("Something")).isFalse();
    }




}
