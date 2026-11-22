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
package org.eclipse.daanse.olap.function.def.nonemptycrossjoin;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.evaluator.NativeEvaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;



public class NonEmptyCrossJoinCalc extends AbstractNestedTupleListCalc{
    private ResolvedFunCall call;
    private int ctag;
    
    public NonEmptyCrossJoinCalc(Type type, TupleListCalc listCalc1, TupleListCalc listCalc2, boolean mutable, final ResolvedFunCall call,  int ctag) {
        super(type, new Calc[] {listCalc1, listCalc2}, mutable);
        this.call = call;
        this.ctag = ctag;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        CatalogReader schemaReader = evaluator.getCatalogReader();

        // Evaluate the arguments in non empty mode, but remove from
        // the slicer any members that will be overridden by args to
        // the NonEmptyCrossjoin function. For example, in
        //
        //   SELECT NonEmptyCrossJoin(
        //       [Store].[USA].Children,
        //       [Product].[Beer].Children)
        //    FROM [Sales]
        //    WHERE [Store].[Mexico]
        //
        // we want all beers, not just those sold in Mexico.
        final int savepoint = evaluator.savepoint();
        final TupleListCalc listCalc1 = getChildCalc(0, TupleListCalc.class);
        final TupleListCalc listCalc2 = getChildCalc(1, TupleListCalc.class);
        try {
            evaluator.setNonEmpty(true);
            for (Member member
                : evaluator.getSlicerMembers())
            {
                if (getType().getElementType().usesHierarchy(
                        member.getHierarchy(), true))
                {
                    evaluator.setContext(
                        member.getHierarchy().getAllMember());
                }
            }

            NativeEvaluator nativeEvaluator =
                schemaReader.getNativeSetEvaluator(
                    call.getFunDef(), call.getArgs(), evaluator, this);
            if (nativeEvaluator != null) {
                evaluator.restore(savepoint);
                return
                    (TupleList) nativeEvaluator.execute(
                        ResultStyle.LIST);
            }

            final TupleList list1 = listCalc1.evaluate(evaluator);
            if (list1.isEmpty()) {
                evaluator.restore(savepoint);
                return list1;
            }
            final TupleList list2 = listCalc2.evaluate(evaluator);
            TupleList result = CrossJoinFunDef.mutableCrossJoin(list1, list2);

            // remove any remaining empty crossings from the result
            result = CrossJoinFunDef.nonEmptyList(evaluator, result, call, ctag);
            return result;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        final TupleListCalc listCalc1 = getChildCalc(0, TupleListCalc.class);
        final TupleListCalc listCalc2 = getChildCalc(1, TupleListCalc.class);
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        // Member calculations generate members, which mask the actual
        // expression from the inherited context.
        if (listCalc1.getType().usesHierarchy(hierarchy, true)) {
            return false;
        }
        if (listCalc2.getType().usesHierarchy(hierarchy, true)) {
            return false;
        }
        // The implicit value expression, executed to figure out
        // whether a given tuple is empty, depends upon all dimensions.
        return true;
    }

}
