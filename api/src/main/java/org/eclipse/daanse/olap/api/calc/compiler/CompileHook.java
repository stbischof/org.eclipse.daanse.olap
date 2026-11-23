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
package org.eclipse.daanse.olap.api.calc.compiler;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * Sees every compiled calc once and hands back the calc to use: the calc itself or a hull around it (profiling, the
 * dependency check). The compiler calls its hooks in order after each compile of an entry, so the first hook's hull is
 * the innermost (before, a decorator compiler rebuilt every entry and only one could be active).
 */
@FunctionalInterface
public interface CompileHook {

    /**
     * @param exp     the expression compiled
     * @param calc    the calc compiled from it, already through the hooks before this one
     * @param request what the caller asked for
     * @return the calc to hand back, of the same kind as {@code calc}
     */
    Calc<?> afterCompile(Expression exp, Calc<?> calc, CompileRequest request);
}
