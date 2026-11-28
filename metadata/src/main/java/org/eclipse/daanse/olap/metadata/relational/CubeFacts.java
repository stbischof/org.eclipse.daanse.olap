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
 */
package org.eclipse.daanse.olap.metadata.relational;

import java.util.List;
import java.util.Optional;

/**
 * Where a physical cube's facts lie: the source its rows come from, the aggregation tables the database keeps
 * summed-up copies in, and the table written values go to. The binding of a cube to tables; a cube whose facts
 * come through a source of its own has none.
 *
 * <p>The aggregation tables hang here and not on the source: a source is a value that is re-aliased and shared, and
 * the aggregation configuration is the cube's, not the alias's.
 *
 * @param source              what the fact rows are read from
 * @param aggregationTables   tables that hold the facts already summed up, matched by name or pattern
 * @param aggregationExcludes tables the pattern matching must leave out
 * @param writeback           where a written value is stored; {@code null} for a cube that takes no writes
 */
public record CubeFacts(RelationalSource source, List<AggregationTableDef> aggregationTables,
        List<AggregationExcludeDef> aggregationExcludes, WritebackTableDef writeback) {

    public CubeFacts {
        aggregationTables = List.copyOf(aggregationTables);
        aggregationExcludes = List.copyOf(aggregationExcludes);
    }

    public static CubeFacts of(RelationalSource source) {
        return new CubeFacts(source, List.of(), List.of(), null);
    }

    public Optional<WritebackTableDef> writebackTable() {
        return Optional.ofNullable(writeback);
    }
}
