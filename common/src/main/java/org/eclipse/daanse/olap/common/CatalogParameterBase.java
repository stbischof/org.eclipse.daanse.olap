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
package org.eclipse.daanse.olap.common;

import java.util.Objects;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.CompilableParameter;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.exceptions.ParameterIsNotModifiableException;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;

/**
 * A parameter a catalog declares, as opposed to one a query declares.
 * <p>
 * Its default is an expression written as text, which is parsed and compiled the first time it is needed
 * and then kept. A value assigned to the parameter wins over that default; assigning to one that was
 * declared unmodifiable is refused by name. A parameter may be declared without a default, and then it
 * answers with its value - or says by name that it has neither.
 * <p>
 * A parameter is read while a query runs and written from wherever a client sits, so its state is held
 * in fields that say so.
 */
public class CatalogParameterBase implements Parameter, CompilableParameter {

    private final String name;
    private final String description;
    private final String defaultExpression;
    private final Type type;
    private final boolean modifiable;
    private volatile Object value;
    private volatile boolean assigned;
    private volatile Object cachedDefault;

    protected CatalogParameterBase(String name, String defaultExpression, String description, Type type,
            boolean modifiable) {
        this.name = Objects.requireNonNull(name, "name");
        this.defaultExpression = defaultExpression;
        this.description = description;
        this.type = Objects.requireNonNull(type, "type");
        this.modifiable = modifiable;
    }

    /** A catalog parameter is in scope for the whole catalog. */
    @Override
    public Scope getScope() {
        return Scope.Schema;
    }

    @Override
    public Type getType() {
        return type;
    }

    /** The default is compiled when it is needed, so there is no expression to hand out. */
    @Override
    public Expression getDefaultExp() {
        throw new UnsupportedOperationException("The default of a catalog parameter is compiled, not handed out");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isModifiable() {
        return modifiable;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object newValue) {
        requireModifiable();
        this.assigned = true;
        this.value = newValue;
    }

    @Override
    public boolean isSet() {
        return assigned;
    }

    @Override
    public void unsetValue() {
        requireModifiable();
        this.assigned = false;
        this.value = null;
    }

    private void requireModifiable() {
        if (!modifiable) {
            throw new ParameterIsNotModifiableException(name, getScope().name());
        }
    }

    /**
     * A program that answers with the assigned value, and otherwise with the default - worked out once and
     * kept, because a default that reads the catalog should not be read again for every cell.
     */
    @Override
    public Calc<?> compile(ExpressionCompiler compiler) {
        if (defaultExpression == null) {
            return new AbstractProfilingNestedUnknownCalc(type, new Calc[0]) {
                @Override
                public Object evaluateInternal(Evaluator evaluator) {
                    if (value == null && !assigned) {
                        throw new OlapRuntimeException("The parameter '" + name
                                + "' was declared without a default and has no value");
                    }
                    return value;
                }
            };
        }
        Expression parsed = compiler.getValidator().getQuery().getConnection().parseExpression(defaultExpression);
        Expression defaultExp = compiler.getValidator().validate(parsed, true);
        Calc<?> defaultCalc = defaultExp.accept(compiler);
        return new AbstractProfilingNestedUnknownCalc(defaultExp.getType(), new Calc[] { defaultCalc }) {
            @Override
            public Object evaluateInternal(Evaluator evaluator) {
                if (value != null) {
                    return value;
                }
                if (cachedDefault == null) {
                    cachedDefault = defaultCalc.evaluate(evaluator);
                }
                return cachedDefault;
            }
        };
    }
}
