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
package org.eclipse.daanse.olap.common;

import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.metadata.model.ValueKind;

/**
 * The type of a catalog parameter from the kind of value its description gives, for every art: text and
 * numbers as String, Numeric or Integer, a truth value as Boolean, a date, a time or a timestamp as a value
 * of time, and no kind at all as a member parameter, of whichever hierarchy each cube resolves the default
 * in.
 */
public final class ParameterTypes {

    private ParameterTypes() {
    }

    public static Type of(ValueKind kind) {
        if (kind == null) {
            return new MemberType(null, null, null, null);
        }
        return switch (kind) {
        case STRING -> StringType.INSTANCE;
        case NUMERIC, INTEGER -> NumericType.INSTANCE;
        case BOOLEAN -> BooleanType.INSTANCE;
        case DATE, TIME, TIMESTAMP -> DateTimeType.INSTANCE;
        };
    }
}
