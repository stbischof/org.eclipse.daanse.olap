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
package org.eclipse.daanse.olap.metadata.builder;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.FindingPolicy;
import org.eclipse.daanse.olap.findings.Findings;
import org.eclipse.daanse.olap.findings.Layer;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.findings.Rule;

/** What a builder finds wrong while it puts a description together, before the validator sees the description. */
final class BuildFindings {

    private BuildFindings() {
    }

    /** The finding of a rule of the description at the place; every such rule stops the build. */
    static Finding of(Rule rule, Place place, String message) {
        return Findings.of(FindingPolicy.NONE, rule, Layer.DESCRIPTION, place, message);
    }
}
