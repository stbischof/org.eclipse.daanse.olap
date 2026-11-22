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
package org.eclipse.daanse.olap.function.def.drilldownleveltopbottom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.evaluator.NativeEvaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.sort.Sorter;

public class DrilldownLevelTopBottomCalc extends AbstractNestedTupleListCalc {

    private final boolean top;
    private final ResolvedFunCall call;
    private final LevelCalc levelCalc;
    private final FunctionMetaData functionMetaData;

    public DrilldownLevelTopBottomCalc(final Type type, final TupleListCalc tupleListCalc,
            final IntegerCalc integerCalc, final Calc<?> orderCalc, final LevelCalc levelCalc, final boolean top,
            final ResolvedFunCall call, final FunctionMetaData functionMetaData) {
        super(type, tupleListCalc, integerCalc, orderCalc);
        this.top = top;
        this.call = call;
        this.levelCalc = levelCalc;
        this.functionMetaData = functionMetaData;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        // Use a native evaluator, if more efficient.
        // TODO: Figure this out at compile time.
        TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        IntegerCalc integerCalc = getChildCalc(1, IntegerCalc.class);
        Calc<?> orderCalc = getChildCalc(2, Calc.class);
        CatalogReader schemaReader = evaluator.getCatalogReader();
        NativeEvaluator nativeEvaluator = schemaReader.getNativeSetEvaluator(call.getFunDef(), call.getArgs(),
                evaluator, this);
        if (nativeEvaluator != null) {
            return (TupleList) nativeEvaluator.execute(ResultStyle.LIST);
        }

        TupleList list = tupleListCalc.evaluate(evaluator);
        Integer n = integerCalc.evaluate(evaluator);
        if (n == null || n <= 0) {
            return list;
        }
        Level level;
        if (levelCalc == null) {
            level = null;
        } else {
            level = levelCalc.evaluate(evaluator);
        }
        List<Member> result = new ArrayList<>();
        assert list.getArity() == 1;
        for (Member member : list.slice(0)) {
            result.add(member);
            if (level != null && member.getLevel() != level) {
                if (level.getDimension() != member.getDimension()) {
                    throw FunUtil.newEvalException(functionMetaData,
                            new StringBuilder("Level '").append(level.getUniqueName())
                                    .append("' not compatible with member '").append(member.getUniqueName()).append("'")
                                    .toString());
                }
                continue;
            }
            List<Member> children = schemaReader.getMemberChildren(member);
            final int savepoint = evaluator.savepoint();
            List<Member> sortedChildren;
            try {
                evaluator.setNonEmpty(false);
                sortedChildren = Sorter.sortMembers(evaluator, children, children, orderCalc, top, true);
            } finally {
                evaluator.restore(savepoint);
            }
            int x = Math.min(n, sortedChildren.size());
            for (int i = 0; i < x; i++) {
                result.add(sortedChildren.get(i));
            }
        }
        return new UnaryTupleList(result);
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

}
