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

import java.util.List;
import java.util.stream.Collectors;

/** The work stops because of findings; carries every one of them, not only the first. */
public class FindingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<Finding> findings;

    /** Stops with these findings, at least one. */
    public FindingException(List<Finding> findings) {
        super(text(findings));
        this.findings = List.copyOf(findings);
    }

    /** The findings the work stops with. */
    public List<Finding> findings() {
        return findings;
    }

    private static String text(List<Finding> findings) {
        if (findings.isEmpty()) {
            throw new IllegalArgumentException("the work stops with at least one finding");
        }
        return findings.size() + " finding(s) stop the work:\n" + findings.stream()
                .map(f -> "  - " + f.place().text() + ": " + f.message() + " (" + f.rule() + ")")
                .collect(Collectors.joining("\n"));
    }
}
