/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.metadata;

import java.util.List;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.metadata.builder.CatalogBuilder;
import org.eclipse.daanse.olap.metadata.model.CatalogDef;

/**
 * Where describing a catalog in code starts.
 *
 * <pre>{@code
 * CatalogDef sales = Olap.catalog("Sales")
 *     .dimension("Time", d -> d.time().hierarchy(h -> h
 *         .level("Year", l -> l.kind(LevelKind.TIME_YEARS))
 *         .level("Month", l -> l.kind(LevelKind.TIME_MONTHS))))
 *     .cube("Sales", c -> c
 *         .dimension("Order Date", "Time")
 *         .dimension("Ship Date", "Time")
 *         .measure("Amount")
 *         .kpi("Growth", k -> k.value("[Measures].[Amount]")))
 *     .build();
 * }</pre>
 */
public final class Olap {

    private Olap() {
    }

    /** Starts the description of a catalog; {@code build()} validates it. */
    public static CatalogBuilder catalog(String name) {
        return new CatalogBuilder(name);
    }

    /** What the {@link DescriptionRules} find in a description put together without the builder; empty when valid. */
    public static List<Finding> validate(CatalogDef catalog) {
        return ModelValidator.validate(catalog);
    }
}
