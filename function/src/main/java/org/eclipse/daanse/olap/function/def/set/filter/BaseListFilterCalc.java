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
package org.eclipse.daanse.olap.function.def.set.filter;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.evaluator.NativeEvaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;

public abstract class BaseListFilterCalc extends AbstractNestedTupleListCalc {
    private ResolvedFunCall call;

    protected BaseListFilterCalc(ResolvedFunCall call, Calc<?>[] calcs) {
        super(call.getType(), calcs);
        this.call=call;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        // Use a native evaluator, if more efficient.
        // TODO: Figure this out at compile time.
        CatalogReader schemaReader = evaluator.getCatalogReader();
        NativeEvaluator nativeEvaluator =
            schemaReader.getNativeSetEvaluator(
                call.getFunDef(), call.getArgs(), evaluator, this);
        if (nativeEvaluator != null) {
            return (TupleList) nativeEvaluator.execute(
                ResultStyle.ITERABLE);
        } else {
            return makeList(evaluator);
        }
    }
    protected abstract TupleList makeList(Evaluator evaluator);

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }
}
