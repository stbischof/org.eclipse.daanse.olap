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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.findings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Where in a catalog a finding sits: a path of steps from the catalog down, each a kind and a name --
 * {@code catalog Shop / cube Sales / measure group Sales / link Shop}. A person finds the element by it, a machine
 * resolves it step by step.
 *
 * <p>In the text form the steps are separated by {@code " / "}; a slash or a backslash in a name is written with a
 * backslash before it, so a name may hold anything. A kind of several words is written as it is when it is one of
 * {@link #KINDS}; another kind of several words has a backslash before each blank, so the text reads back.
 *
 * @param steps the steps from the catalog down, at least one
 */
public record Place(List<Step> steps) {

    /**
     * The kinds of several words the text form knows without escaping, longest first. A kind of one word needs no
     * entry.
     */
    public static final List<String> KINDS = List.of("calculated member", "member property", "measure group",
            "aggregation table", "writeback table", "named set", "cube dimension", "group dimension");

    /**
     * One step: the kind of the element and its name.
     *
     * @param kind the kind, a word of the checks: catalog, cube, measure group, dimension, hierarchy, level, measure,
     *             link, role, named set, KPI, action, perspective, table, column and others
     * @param name the name, as the catalog has it; an element without a name has the empty name
     */
    public record Step(String kind, String name) {

        public Step {
            Objects.requireNonNull(kind, "the kind of a step of a place");
            name = name == null ? "" : name;
            if (kind.isBlank()) {
                throw new IllegalArgumentException("a step of a place has no kind");
            }
            if (kind.indexOf('/') >= 0 || kind.indexOf('\\') >= 0 || !kind.equals(kind.strip())) {
                throw new IllegalArgumentException("the kind '" + kind + "' of a step of a place holds a slash, a"
                        + " backslash or a blank at its edge");
            }
        }

        /** The step as the text form writes it. */
        String text() {
            String written = KINDS.contains(kind) ? kind : kind.replace(" ", "\\ ");
            String step = written + " " + name.replace("\\", "\\\\").replace("/", "\\/");
            if (!KINDS.contains(kind) && KINDS.stream().anyMatch(known -> step.startsWith(known + " "))) {
                // a kind of one word whose name reads on as a kind of several: the name's first sign escaped
                return written + " \\" + step.substring(written.length() + 1);
            }
            return step;
        }
    }

    public Place {
        Objects.requireNonNull(steps, "the steps of a place");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("a place has at least one step");
        }
        steps = List.copyOf(steps);
    }

    /** A place of one step. */
    public static Place of(String kind, String name) {
        return new Place(List.of(new Step(kind, name)));
    }

    /** The place one step further down. */
    public Place then(String kind, String name) {
        List<Step> longer = new ArrayList<>(steps);
        longer.add(new Step(kind, name));
        return new Place(longer);
    }

    /** The place one step up; none for a place of one step. */
    public Optional<Place> parent() {
        return steps.size() == 1 ? Optional.empty() : Optional.of(new Place(steps.subList(0, steps.size() - 1)));
    }

    /** The last step: the element itself. */
    public Step last() {
        return steps.get(steps.size() - 1);
    }

    /** Whether this place is the scope or lies below it. */
    public boolean within(Place scope) {
        return scope.steps.size() <= steps.size() && steps.subList(0, scope.steps.size()).equals(scope.steps);
    }

    /** Whether a step of this kind is on the way, for example a level of the place. */
    public boolean passes(String kind) {
        return steps.stream().anyMatch(step -> step.kind().equals(kind));
    }

    /** The text form: the steps separated by {@code " / "}. */
    public String text() {
        StringBuilder text = new StringBuilder();
        for (Step step : steps) {
            if (!text.isEmpty()) {
                text.append(" / ");
            }
            text.append(step.text());
        }
        return text.toString();
    }

    /** The place the text form writes. */
    public static Place parse(String text) {
        Objects.requireNonNull(text, "the text of a place");
        List<Step> steps = new ArrayList<>();
        // split at the separators no backslash escapes, keeping the escapes for the step to read
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '/' && i > 0 && text.charAt(i - 1) == ' ' && i + 1 < text.length()
                    && text.charAt(i + 1) == ' ') {
                steps.add(step(text, text.substring(start, i - 1)));
                start = i + 2;
            }
        }
        steps.add(step(text, text.substring(start)));
        return new Place(steps);
    }

    private static Step step(String text, String written) {
        String kind = null;
        int nameStart = -1;
        for (String known : KINDS) {
            if (written.startsWith(known + " ")) {
                kind = known;
                nameStart = known.length() + 1;
                break;
            }
        }
        if (kind == null) {
            StringBuilder word = new StringBuilder();
            for (int i = 0; i < written.length(); i++) {
                char c = written.charAt(i);
                if (c == '\\' && i + 1 < written.length() && written.charAt(i + 1) == ' ') {
                    word.append(' ');
                    i++;
                } else if (c == ' ') {
                    nameStart = i + 1;
                    break;
                } else {
                    word.append(c);
                }
            }
            kind = word.toString();
        }
        if (nameStart < 0 || kind.isEmpty()) {
            throw new IllegalArgumentException("the step '" + written + "' of the place '" + text
                    + "' is not a kind and a name separated by a blank");
        }
        StringBuilder name = new StringBuilder();
        for (int i = nameStart; i < written.length(); i++) {
            char c = written.charAt(i);
            if (c == '\\' && i + 1 < written.length()) {
                name.append(written.charAt(++i));
            } else {
                name.append(c);
            }
        }
        return new Step(kind, name.toString());
    }

    @Override
    public String toString() {
        return text();
    }
}
