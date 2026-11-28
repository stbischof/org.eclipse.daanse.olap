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
package org.eclipse.daanse.olap.metadata;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.FindingException;
import org.eclipse.daanse.olap.findings.Layer;
import org.eclipse.daanse.olap.findings.Rule;
import org.eclipse.daanse.olap.findings.Severity;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;

/**
 * Reads what a catalog description is blamed for, whether the builder threw or the validator was asked, field by
 * field: the rule, the place, the severity, the layer, and a part of the message. Prints every finding when an
 * expected one is missing, for example {@code assertThatModel(() -> catalog).fails().withProblem(DescriptionRules.M039,
 * "catalog C / cube Sales", "has no measure")}.
 */
public class FindingAssert extends AbstractAssert<FindingAssert, List<Finding>> {

    FindingAssert(List<Finding> found) {
        super(found, FindingAssert.class);
    }

    /** Validates a description that is already built. */
    public static FindingAssert assertThatModel(CatalogDef description) {
        return new FindingAssert(Olap.validate(description));
    }

    /** Builds a description and collects what the build blamed it for, if anything. */
    public static FindingAssert assertThatModel(Supplier<CatalogDef> description) {
        try {
            return assertThatModel(description.get());
        } catch (FindingException e) {
            return new FindingAssert(e.findings());
        }
    }

    /** Nothing is wrong with the description. */
    public FindingAssert isValid() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("%s", report("a description without a single problem", actual.size() + " problem(s)"));
        }
        return this;
    }

    /** Something is wrong with the description. */
    public FindingAssert fails() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("%s", report("at least one problem", "a description the validator accepts"));
        }
        return this;
    }

    /** Exactly one finding of the rule that stops the build sits at this place and says this. */
    public FindingAssert withProblem(Rule rule, String place, String message) {
        isNotNull();
        long matching = actual.stream().filter(is(rule, Severity.ERROR, place, message)).count();
        if (matching != 1) {
            failWithMessage("%s", report("one problem " + rule.id() + " at " + quoted(place) + " saying "
                    + quoted(message), matching == 0 ? "no such problem" : matching + " such problems"));
        }
        return this;
    }

    private static Predicate<Finding> is(Rule rule, Severity severity, String place, String message) {
        return finding -> finding.rule().equals(rule.id()) && finding.severity() == severity
                && finding.layer() == Layer.DESCRIPTION && finding.place().text().equals(place)
                && finding.message().contains(message);
    }

    /** The description is blamed for exactly this many things. */
    public FindingAssert withProblemCount(int expected) {
        isNotNull();
        if (actual.size() != expected) {
            failWithMessage("%s", report(expected + " problem(s)", actual.size() + " problem(s)"));
        }
        return this;
    }

    /** The message a caller of the description would see names every problem with its place and rule. */
    public FindingAssert withAMessageNamingEveryProblem() {
        isNotNull();
        String message = new FindingException(actual).getMessage();
        List<Finding> missing = actual.stream()
                .filter(finding -> !message.contains(finding.place().text() + ": " + finding.message() + " ("
                        + finding.rule() + ")"))
                .toList();
        if (!missing.isEmpty() || !message.contains(actual.size() + " finding(s)")) {
            failWithMessage("%s", report("a message naming all " + actual.size() + " problem(s)", message));
        }
        return this;
    }

    private String report(String expected, String found) {
        StringBuilder text = new StringBuilder();
        text.append("expected: ").append(expected).append(System.lineSeparator());
        text.append(" but was: ").append(found).append(System.lineSeparator());
        text.append(String.format("  %-3s %-18s %-64s %s%n", "no", "rule", "place", "message"));
        if (actual.isEmpty()) {
            text.append(String.format("  %s%n", "(the validator found nothing to blame)"));
        }
        int number = 1;
        for (Finding finding : actual) {
            text.append(String.format("  %-3d %-18s %-64s %s%n", number++, finding.rule(), finding.place().text(),
                    finding.message()));
        }
        return text.toString();
    }

    private static String quoted(String text) {
        return "'" + text + "'";
    }
}
