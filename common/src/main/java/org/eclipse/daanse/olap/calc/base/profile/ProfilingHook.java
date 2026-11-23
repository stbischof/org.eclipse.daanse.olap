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
package org.eclipse.daanse.olap.calc.base.profile;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.CompileHook;
import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import org.eclipse.daanse.olap.api.calc.profile.ProfileCounters;
import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * The hook of a profiled statement: every compiled calc comes back in a {@link ProfilingCalc} hull that measures it,
 * named after the function of its expression. Decided once per statement (QueryImpl.createCompiler), so a statement
 * without a profile handler pays nothing in its evaluations. It runs first, innermost: the hull measures the calc, not
 * the dependency check around it.
 */
public class ProfilingHook implements CompileHook {

    private final ProfileCounters counters;

    /** A hook whose hulls also count every evaluation in {@code counters}, or only time it when that is null. */
    public ProfilingHook(ProfileCounters counters) {
        this.counters = counters;
    }

    @Override
    public Calc<?> afterCompile(Expression exp, Calc<?> calc, CompileRequest request) {
        return ProfilingCalc.wrap(calc,
                exp instanceof org.eclipse.daanse.olap.api.query.component.FunctionCall call
                        ? call.getOperationAtom().name()
                        : null,
                counters);
    }
}
