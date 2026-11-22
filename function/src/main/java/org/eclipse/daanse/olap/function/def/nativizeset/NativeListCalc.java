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
package org.eclipse.daanse.olap.function.def.nativizeset;

import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.common.Util;

public class NativeListCalc  extends AbstractNestedTupleListCalc {
    private final SubstitutionMap substitutionMap;
    private final TupleListCalc simpleCalc;
    private final ExpressionCompiler compiler;

    private final Expression originalExp;

    protected NativeListCalc(
        Type type,
        Calc<?>[] calcs,
        ExpressionCompiler compiler,
        SubstitutionMap substitutionMap,
        Expression originalExp)
    {
        super(type, calcs);
        NativizeSetFunDef.LOGGER.debug("---- NativeListCalc constructor");
        this.substitutionMap = substitutionMap;
        this.simpleCalc = (TupleListCalc) calcs[0];
        this.compiler = compiler;
        this.originalExp = originalExp;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        return computeTuples(evaluator);
    }

    public TupleList computeTuples(Evaluator evaluator) {
        TupleList simplifiedList = evaluateSimplifiedList(evaluator);
        if (simplifiedList.isEmpty()) {
            return simplifiedList;
        }
        if (!isHighCardinality(evaluator, simplifiedList)) {
            return evaluateNonNative(evaluator);
        }
        return evaluateNative(evaluator, simplifiedList);
    }

    private TupleList evaluateSimplifiedList(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            evaluator.setNativeEnabled(false);
            TupleList simplifiedList =
                    simpleCalc.evaluate(evaluator);
            NativizeSetFunDef.dumpListToLog("simplified list", simplifiedList);
            return simplifiedList;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private TupleList evaluateNonNative(Evaluator evaluator) {
        NativizeSetFunDef.LOGGER.debug(
            "Disabling native evaluation. originalExp="
                + originalExp);
        TupleListCalc calc =
            compiler.compileList(getOriginalExp(evaluator.getQuery()));
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(true);
            evaluator.setNativeEnabled(false);
            TupleList members = calc.evaluate(evaluator);
            return members;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private TupleList evaluateNative(
        Evaluator evaluator, TupleList simplifiedList)
    {
        CrossJoinAnalyzer analyzer =
            new CrossJoinAnalyzer(simplifiedList, substitutionMap,
                evaluator.getQuery().getConnection().getContext().getConfig().nativizeMaxResults());
        String crossJoin = analyzer.getCrossJoinExpression();

        // If the crossjoin expression is empty, then the simplified list
        // already contains the fully evaluated tuple list, so we can
        // return it now without any additional work.
        if (crossJoin.length() == 0) {
            return simplifiedList;
        }

        // Force non-empty to true to create the native list.
        NativizeSetFunDef.LOGGER.debug(
            "crossjoin reconstituted from simplified list: "
            + String.format(
                "%n"
                + crossJoin.replaceAll(",", "%n, ")));
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(true);
            evaluator.setNativeEnabled(true);

            TupleList members = analyzer.mergeCalcMembers(
                evaluateJoinExpression(evaluator, crossJoin));
            return members;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private Expression getOriginalExp(final Query query) {
        originalExp.accept(
            new TransformFromFormulasVisitor(query, compiler));
        if (originalExp instanceof NamedSetExpression namedSetExpr) {
            //named sets get their evaluator cached in RolapResult.
            //We do not want to use the cached evaluator, so pass along the
            //expression instead.
            return namedSetExpr.getNamedSet().getExp();
        }
        return originalExp;
    }

    private boolean isHighCardinality(
        Evaluator evaluator, TupleList simplifiedList)
    {
        Util.assertTrue(!simplifiedList.isEmpty());

        CatalogReader schema = evaluator.getCatalogReader();
        List<Member> tuple = simplifiedList.get(0);
        long nativizeMinThreshold =
            evaluator.getQuery().getConnection().getContext()
            .getConfig().nativizeMinThreshold();
        long estimatedCardinality = simplifiedList.size();

        for (Member member : tuple) {
            String memberName = member.getName();
            if (memberName.startsWith(NativizeSetFunDef.MEMBER_NAME_PREFIX)) {
                Level level = member.getLevel();
                Dimension dimension = level.getDimension();
                Hierarchy hierarchy = dimension.getHierarchy();

                String levelName = NativizeSetFunDef.getLevelNameFromMemberName(memberName);
                Level hierarchyLevel =
                    Util.lookupHierarchyLevel(hierarchy, levelName);
                long levelCardinality =
                    getLevelCardinality(schema, hierarchyLevel);
                estimatedCardinality *= levelCardinality;
                if (estimatedCardinality >= nativizeMinThreshold) {
                    NativizeSetFunDef.logHighCardinality(
                        NativizeSetFunDef.PARTIAL_ESTIMATE_MESSAGE,
                        nativizeMinThreshold,
                        estimatedCardinality,
                        true);
                    return true;
                }
            }
        }

        boolean isHighCardinality =
            (estimatedCardinality >= nativizeMinThreshold);

        NativizeSetFunDef.logHighCardinality(
            NativizeSetFunDef.ESTIMATE_MESSAGE,
            nativizeMinThreshold,
            estimatedCardinality,
            isHighCardinality);
        return isHighCardinality;
    }

    private long getLevelCardinality(CatalogReader schema, Level level) {
        if (cardinalityIsKnown(level)) {
            return level.getApproxRowCount();
        }
        return schema.getLevelCardinality(level, false, true);
    }

    private boolean cardinalityIsKnown(Level level) {
        return level.getApproxRowCount() > 0;
    }

    private TupleList evaluateJoinExpression(
        Evaluator evaluator, String crossJoinExpression)
    {
        Expression unresolved =
            evaluator.getQuery().getConnection()
                .parseExpression(crossJoinExpression);
        Expression resolved = compiler.getValidator().validate(unresolved, false);
        TupleListCalc calc = compiler.compileList(resolved);
        return calc.evaluate(evaluator);
    }
}
