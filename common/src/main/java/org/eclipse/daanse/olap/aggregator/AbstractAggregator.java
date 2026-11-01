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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.aggregator;

import java.util.List;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

/**
 * Base of the aggregators: carries the name and the distinct flag, rolls up with itself and
 * refuses to aggregate raw values. Subclasses override what they support.
 */
public abstract class AbstractAggregator implements Aggregator {

    protected final String name;
    private final boolean distinct;

    protected AbstractAggregator(String name, boolean distinct) {
        this.name = name;
        this.distinct = distinct;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public Aggregator getNonDistinctAggregator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Aggregator getRollup() {
        return this;
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        return false;
    }

    @Override
    public Object aggregate(List<Object> rawData, DataTypeJdbc datatype) {
        throw new UnsupportedOperationException();
    }

    /** Whether raw values of the datatype are numbers this package aggregates directly. */
    protected static boolean isNumber(DataTypeJdbc dataType) {
        return switch (dataType) {
        case INTEGER, NUMERIC -> true;
        default -> false;
        };
    }

    protected OlapRuntimeException unsupportedDatatype(DataTypeJdbc datatype) {
        return new OlapRuntimeException(new StringBuilder("Aggregator ").append(name)
                .append(" does not support datatype").append(datatype.getValue()).toString());
    }

    @Override
    public String toString() {
        return name;
    }
}
