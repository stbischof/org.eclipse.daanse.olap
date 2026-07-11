/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.nonstandard;

import java.util.Date;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.util.type.TypeUtil;

public class CastCalc extends AbstractProfilingNestedUnknownCalc {
    private final Calc<?> calc;
    private final Type targetType;
    private final DataType targetCategory;

    public CastCalc(Expression arg, Calc<?> calc, Type targetType) {
        super(arg.getType());
        this.calc = calc;
        this.targetType = targetType;
        this.targetCategory = TypeUtil.typeToCategory(targetType);
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { calc };
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        switch (targetCategory) {
        case STRING:
            return evaluateString(evaluator);
        case INTEGER:
            return evaluateInteger(evaluator);
        case NUMERIC:
            return evaluateDouble(evaluator);
        case DATE_TIME:
            return evaluateDateTime(evaluator);
        case LOGICAL:
            return evaluateBoolean(evaluator);
        default:
            throw Util.newInternal("category " + targetCategory);
        }
    }

    public String evaluateString(Evaluator evaluator) {
        final Object o = calc.evaluate(evaluator);
        if (o == null) {
            return null;
        }
        return String.valueOf(o);
    }

    public Integer evaluateInteger(Evaluator evaluator) {
        final Object o = calc.evaluate(evaluator);
        return toInt(o, targetType);
    }

    public Double evaluateDouble(Evaluator evaluator) {
        final Object o = calc.evaluate(evaluator);
        return toDouble(o, targetType);
    }

    public boolean evaluateBoolean(Evaluator evaluator) {
        final Object o = calc.evaluate(evaluator);
        return toBoolean(o, targetType);
    }

    public Date evaluateDateTime(Evaluator evaluator) {
        final Object o = calc.evaluate(evaluator);
        if (o instanceof Date d) {
            return d;
        }
        throw evaluator.newEvalException(null, "must be Date but was: " + o);
    }

    private Integer toInt(Object o, final Type targetType) {
        if (o == null) {
            return null;
        }
        if (o instanceof String str) {
            return Integer.parseInt(str);
        }
        if (o instanceof Number number) {
            return number.intValue();
        }
        throw cannotConvert(o, targetType);
    }

    private Double toDouble(Object o, final Type targetType) {
        if (o == null) {
            // Cast(NULL as Numeric) is NULL — Java null.
            return null;
        }
        if (o instanceof Boolean bool) {
            if (Boolean.TRUE.equals(bool)) {
                return 1.0;
            } else {
                return 0.0;
            }
        }
        if (o instanceof String str) {
            return Double.valueOf(str);
        }
        if (o instanceof Number number) {
            return number.doubleValue();
        }
        throw cannotConvert(o, targetType);
    }

    private boolean toBoolean(Object o, final Type targetType) {
        if (o == null) {
            return FunUtil.BOOLEAN_NULL;
        }
        if (o instanceof Boolean bool) {
            return bool;
        }
        if (o instanceof String str) {
            return Boolean.valueOf(str);
        }
        if (o instanceof Number number) {
            return number.doubleValue() > 0;
        }
        throw cannotConvert(o, targetType);
    }

    private RuntimeException cannotConvert(Object o, final Type targetType) {
        return Util.newInternal(new StringBuilder("cannot convert value '").append(o).append("' to targetType '")
                .append(targetType).append("'").toString());
    }

}
