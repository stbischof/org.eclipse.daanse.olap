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

import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;
import org.eclipse.daanse.olap.query.component.NullLiteralImpl;

/**
 * A key the configuration of a context sets, readable as a parameter of the scope System: the last step of
 * the lookup of a parameter name, after the parameters of the catalog, for every art. The value is
 * the configured one as text; a key that is not set is no parameter, whatever its default, and the
 * parameter cannot be set.
 *
 * <p>The original read the JVM-wide Mondrian properties; a context knows its own configuration.
 */
public final class ConfigurationParameter extends ParameterImpl {

    private final Context<?> context;

    private ConfigurationParameter(Context<?> context, String name) {
        super(name, NullLiteralImpl.nullValue, "System property '" + name + "'", StringType.INSTANCE);
        this.context = context;
    }

    /** The parameter for a key the configuration sets, or empty when it does not set it. */
    public static Optional<ConfigurationParameter> of(Context<?> context, String name) {
        if (context == null || context.getConfigValue(name, null, Object.class) == null) {
            return Optional.empty();
        }
        return Optional.of(new ConfigurationParameter(context, name));
    }

    @Override
    public Scope getScope() {
        return Scope.System;
    }

    @Override
    public boolean isModifiable() {
        return false;
    }

    @Override
    public Calc compile(ExpressionCompiler compiler) {
        return new AbstractProfilingNestedUnknownCalc(getType()) {
            @Override
            public Calc[] getChildCalcs() {
                return new Calc[0];
            }

            @Override
            public Object evaluateInternal(Evaluator evaluator) {
                Object value = context.getConfigValue(getName(), null, Object.class);
                return value == null ? null : String.valueOf(value);
            }
        };
    }
}
