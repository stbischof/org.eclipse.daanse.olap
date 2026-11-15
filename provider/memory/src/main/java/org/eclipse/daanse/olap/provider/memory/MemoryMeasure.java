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
package org.eclipse.daanse.olap.provider.memory;

import org.eclipse.daanse.olap.aggregator.SumAggregator;
import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.element.Measure;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.common.StandardProperty;

/**
 * A member of the measures hierarchy, stored or calculated. The engine tells a measure from any
 * other member by this type, not by the dimension it sits in.
 */
public class MemoryMeasure extends MemoryMember implements Measure {

    MemoryMeasure(String name, MemoryLevel level, MemoryMember parent, Member.MemberType memberType) {
        super(name, level, parent, memberType);
        if (memberType == Member.MemberType.MEASURE) {
            aggregator(SumAggregator.INSTANCE);
        }
    }

    /**
     * How the engine rolls this measure up when it aggregates over a set, as {@code Aggregate}
     * and a compound slicer do. A stored measure sums unless told otherwise.
     */
    public MemoryMeasure aggregator(Aggregator aggregator) {
        setProperty(StandardProperty.AGGREGATION_TYPE.getName(), aggregator);
        return this;
    }
}
