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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.evaluator.EvaluatorImpl;
import org.eclipse.daanse.olap.evaluator.EvaluatorRoot;

/**
 * The evaluation root of this provider.
 *
 * <p>It answers the three questions the engine asks of a root: which evaluator the slicer
 * established, how a compiled expression is evaluated, and whether any value is still
 * owed. Here the last answer is always no, because this provider never defers: it knows
 * every value the moment it is asked.
 */
public class MemoryEvaluatorRoot extends EvaluatorRoot {

    private EvaluatorImpl slicer;

    public MemoryEvaluatorRoot(Statement statement) {
        super(statement);
    }

    /**
     * The root of a running execution.
     *
     * <p>Preferred over the statement-only form wherever an execution exists. Functions
     * ask the evaluator for the query timing of its execution, and a root without one
     * makes them fail with a null pointer rather than with an answer.
     */
    public MemoryEvaluatorRoot(Execution execution) {
        super(execution);
    }

    /** Records the evaluator the slicer axis established. */
    void slicerEvaluator(EvaluatorImpl evaluator) {
        this.slicer = evaluator;
    }

    @Override
    public EvaluatorImpl slicerEvaluator() {
        return slicer;
    }

    @Override
    public Object evaluateExpression(Calc<?> calc, EvaluatorImpl slicerEvaluator,
            Evaluator contextEvaluator) {
        Evaluator context = contextEvaluator != null ? contextEvaluator : slicerEvaluator;
        return calc.evaluate(context);
    }
}
