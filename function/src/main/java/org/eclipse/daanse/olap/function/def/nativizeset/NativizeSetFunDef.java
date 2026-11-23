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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.cache.CacheFunDef;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;
import org.eclipse.daanse.olap.function.def.set.SetFunDef;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativizeSetFunDef extends AbstractFunctionDefinition {
    /*
     * Static final fields.
     */
    protected static final Logger LOGGER =
        LoggerFactory.getLogger(NativizeSetFunDef.class);

    static final String SENTINEL_PREFIX = "_Nativized_Sentinel_";
    static final String MEMBER_NAME_PREFIX = "_Nativized_Member_";
    static final String SET_NAME_PREFIX = "_Nativized_Set_";
    static final List<Class<? extends FunctionDefinition>> functionWhitelist =
        Arrays.<Class<? extends FunctionDefinition>>asList(
            CacheFunDef.class,
            SetFunDef.class,
            CrossJoinFunDef.class,
            NativizeSetFunDef.class);


    /*
     * Instance final fields.
     */
    private final SubstitutionMap substitutionMap = new SubstitutionMap();
    private final HashSet<Dimension> dimensions =
        new LinkedHashSet<>();

    private boolean isFirstCompileCall = true;

    /*
     * Instance non-final fields.
     */
    private Expression originalExp;
    static final String ESTIMATE_MESSAGE =
        "isHighCardinality=%b: estimate=%,d threshold=%,d";
    static final String PARTIAL_ESTIMATE_MESSAGE =
        "isHighCardinality=%b: partial estimate=%,d threshold=%,d";

    public NativizeSetFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
        NativizeSetFunDef.LOGGER.debug("---- NativizeSetFunDef constructor");
    }

    @Override
    public Expression createCall(Validator validator, Expression[] args) {
        NativizeSetFunDef.LOGGER.debug("NativizeSetFunDef createCall");
        ResolvedFunCallImpl call =
            (ResolvedFunCallImpl) super.createCall(validator, args);
        call.accept(new FindLevelsVisitor(substitutionMap, dimensions));
        return call;
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        NativizeSetFunDef.LOGGER.debug("NativizeSetFunDef compileCall");
        Expression funArg = call.getArg(0);

        if (compiler.getEvaluator().getQuery().getConnection().getContext().getConfig().useAggregates()
            || compiler.getEvaluator().getQuery().getConnection().getContext().getConfig().readAggregates())
        {
            return funArg.accept(compiler);
        }

        final Calc<?>[] calcs = {compiler.compileMutableList(funArg)};

        final int arity = calcs[0].getType().getArity();
        assert arity >= 0;
        if (arity == 1 || substitutionMap.isEmpty()) {
          Calc<?>   calc =  funArg.accept(compiler);
            final boolean highCardinality =
                arity == 1
                && isHighCardinality(funArg, compiler.getEvaluator());
            if (calc == null) {
                // This can happen under JDK1.4: caller wants iterator
                // implementation, but compiler can only provide list.
                // Fall through and use native.
            } else if (calc instanceof TupleListCalc tlc) {
                return new NonNativeListCalc(tlc, highCardinality);
            } else  if(calc instanceof TupleIteratorCalc tic) {
                return new NonNativeIterCalc(tic, highCardinality);
            }
        }
        if (isFirstCompileCall) {
            isFirstCompileCall = false;
            originalExp = funArg.cloneExp();
            Query query = compiler.getEvaluator().getQuery();
            call.accept(
                new AddFormulasVisitor(query, substitutionMap, dimensions));
            call.accept(new TransformToFormulasVisitor(query));
            query.resolve();
        }
        return new NativeListCalc(
            call.getType(), calcs, compiler, substitutionMap, originalExp);
    }

    private boolean isHighCardinality(Expression funArg, Evaluator evaluator) {
        Level level = findLevel(funArg);
        if (level != null) {
            int cardinality =
                evaluator.getCatalogReader()
                    .getLevelCardinality(level, false, true);
            final int minThreshold = evaluator.getQuery().getConnection().getContext()
                .getConfig().nativizeMinThreshold();
            final boolean isHighCard = cardinality > minThreshold;
            NativizeSetFunDef.logHighCardinality(
                NativizeSetFunDef.ESTIMATE_MESSAGE, minThreshold, cardinality, isHighCard);
            return isHighCard;
        }
        return false;
    }

    private Level findLevel(Expression exp) {
        exp.accept(new FindLevelsVisitor(substitutionMap, dimensions));
        final Collection<Level> levels = substitutionMap.values();
        if (levels.size() == 1) {
            return levels.iterator().next();
        }
        return null;
    }

    static void logHighCardinality(
        final String estimateMessage,
        long nativizeMinThreshold,
        long estimatedCardinality,
        boolean highCardinality)
    {
        NativizeSetFunDef.LOGGER.debug(
            String.format(
                estimateMessage,
                highCardinality,
                estimatedCardinality,
                nativizeMinThreshold));
    }

    static Id createSentinelId(Level level) {
        return NativizeSetFunDef.hierarchyId(level)
            .append(NativizeSetFunDef.q(NativizeSetFunDef.createMangledName(level, NativizeSetFunDef.SENTINEL_PREFIX)));
    }

    static Id createMemberId(Level level) {
        return NativizeSetFunDef.hierarchyId(level)
            .append(NativizeSetFunDef.q(NativizeSetFunDef.createMangledName(level, NativizeSetFunDef.MEMBER_NAME_PREFIX)));
    }

    static Id createSetId(Level level) {
        return new IdImpl(
            NativizeSetFunDef.q(NativizeSetFunDef.createMangledName(level, NativizeSetFunDef.SET_NAME_PREFIX)));
    }

    static IdImpl hierarchyId(Level level) {
        IdImpl id = new IdImpl(NativizeSetFunDef.q(level.getDimension().getName()));
        id = id.append(NativizeSetFunDef.q(level.getHierarchy().getName()));
        return id;
    }

    private static Segment q(String s) {
        return new IdImpl.NameSegmentImpl(s);
    }

    private static String createMangledName(Level level, String prefix) {
        return new StringBuilder(prefix)
            .append(level.getUniqueName().replaceAll("[\\[\\]]", "")
            .replaceAll("\\.", "_"))
            .append("_").toString();
    }

    static void dumpListToLog(
        String heading, TupleList list)
    {
        if (NativizeSetFunDef.LOGGER.isDebugEnabled()) {
            NativizeSetFunDef.LOGGER.debug(
                String.format(
                    "%s created with %,d rows.", heading, list.size()));
            StringBuilder buf = new StringBuilder(Util.NL);
            for (List<Member> element : list) {
                buf.append(Util.NL);
                buf.append(element);
            }
            NativizeSetFunDef.LOGGER.debug(buf.toString());
        }
    }

    static <T> String toCsv(Collection<T> list) {
        StringBuilder buf = new StringBuilder();
        String sep = "";
        for (T element : list) {
            buf.append(sep).append(element);
            sep = ", ";
        }
        return buf.toString();
    }

    static String getLevelNameFromMemberName(String memberName) {
        // we assume that the last token is the level name
        String tokens[] = memberName.split("_");
        return tokens[tokens.length - 1];
    }
}
