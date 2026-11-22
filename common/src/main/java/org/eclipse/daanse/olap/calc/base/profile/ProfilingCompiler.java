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
package org.eclipse.daanse.olap.calc.base.profile;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.calc.base.compiler.DelegatingExpressionCompiler;

/**
 * The compiler of a profiled statement: every compiled calc comes back in a {@link ProfilingCalc} hull that measures
 * it, named after the function of its expression. Decided once per statement (QueryImpl.createCompiler), so a
 * statement without a profile handler pays nothing in its evaluations; the pattern of the dependency check compiler.
 */
public class ProfilingCompiler extends DelegatingExpressionCompiler {

    public ProfilingCompiler(ExpressionCompiler compiler) {
        super(compiler);
    }

    @Override
    protected Calc<?> afterCompile(Expression exp, Calc<?> calc, boolean mutable) {
        return ProfilingCalc.wrap(calc, exp instanceof FunctionCall call ? call.getOperationAtom().name() : null);
    }
}
