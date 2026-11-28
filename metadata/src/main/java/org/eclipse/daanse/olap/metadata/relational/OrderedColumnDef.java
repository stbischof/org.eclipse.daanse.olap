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

/**
 * A column and the direction rows are ordered by it: what a level sorts its members by, or what an aggregator that
 * takes the n-th or concatenates reads its rows in.
 */
public record OrderedColumnDef(ColumnDef column, Direction direction) {

    public enum Direction {
        ASC, DESC
    }

    public OrderedColumnDef {
        direction = direction == null ? Direction.ASC : direction;
    }

    public static OrderedColumnDef ascending(ColumnDef column) {
        return new OrderedColumnDef(column, Direction.ASC);
    }
}
