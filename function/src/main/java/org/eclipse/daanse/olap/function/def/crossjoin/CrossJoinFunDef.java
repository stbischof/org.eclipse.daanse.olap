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
package org.eclipse.daanse.olap.function.def.crossjoin;

import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleCursor;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.execution.Execution;
import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ListTupleList;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.exceptions.ResultStyleException;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.MemberExtractingVisitor;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.set.ExprIterCalc;
import org.eclipse.daanse.olap.function.def.set.SetListCalc;
import org.eclipse.daanse.olap.util.CancellationChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossJoinFunDef extends AbstractFunctionDefinition {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossJoinFunDef.class);

    private static int counterTag = 0;

    // used to tell the difference between crossjoin expressions.
    private final int ctag = CrossJoinFunDef.counterTag++;

    public CrossJoinFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        // CROSSJOIN(<Set1>,<Set2>) has type [Hie1] x [Hie2].
        List<MemberType> list = new ArrayList<>();
        for (Expression arg : args) {
            final Type type = arg.getType();
            if (type instanceof SetType) {
                CrossJoinFunDef.addTypes(type, list);
            } else if (getFunctionMetaData().operationAtom().name().equals("*")) {
                // The "*" form of CrossJoin is lenient: args can be either
                // members/tuples or sets.
                CrossJoinFunDef.addTypes(type, list);
            } else if (getFunctionMetaData().operationAtom().name().equals("()")) {
                // The "()" form of CrossJoin is lenient: args can be either
                // members/tuples or sets.
                CrossJoinFunDef.addTypes(type, list);
            } else {
                throw Util.newInternal("arg to crossjoin must be a set");
            }
        }
        final MemberType[] types = list.toArray(MemberType[]::new);
        TupleType.checkHierarchies(types);
        final TupleType tupleType = new TupleType(types);
        return new SetType(tupleType);
    }

    /**
     * Adds a type to a list of types. If type is a {@link TupleType}, does so
     * recursively.
     *
     * @param type Type to add to list
     * @param list List of types to add to
     */
    private static void addTypes(final Type type, List<MemberType> list) {
        if (type instanceof SetType setType) {
            CrossJoinFunDef.addTypes(setType.getElementType(), list);
        } else if (type instanceof TupleType tupleType) {
            for (Type elementType : tupleType.elementTypes) {
                CrossJoinFunDef.addTypes(elementType, list);
            }
        } else if (type instanceof MemberType memberType) {
            list.add(memberType);
        } else {
            throw Util.newInternal("Unexpected type: " + type);
        }
    }

    @Override
    public Calc compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
        // What is the desired return type?
        for (ResultStyle r : compiler.getAcceptableResultStyles()) {
            switch (r) {
            case ITERABLE, ANY:
                // Consumer wants ITERABLE or ANY
                return compileCallIterable(call, compiler);
            case LIST:
                // Consumer wants (immutable) LIST
                return compileCallImmutableList(call, compiler);
            case MUTABLE_LIST:
                // Consumer MUTABLE_LIST
                return compileCallMutableList(call, compiler);
            }
        }
        throw ResultStyleException.generate(ResultStyle.ITERABLE_LIST_MUTABLELIST_ANY,
                compiler.getAcceptableResultStyles());
    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // Iterable
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    protected TupleIteratorCalc compileCallIterable(final ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        Calc[] calcs = new Calc[args.length];
        for (int i = 0; i < args.length; i++) {
            calcs[i] = toIter(compiler, args[i]);
        }
        return compileCallIterableArray(call, compiler, calcs);
    }

    protected TupleIteratorCalc compileCallIterableArray(final ResolvedFunCall call, ExpressionCompiler compiler,
            Calc[] calcs) {
        TupleIteratorCalc tupleIteratorCalc = compileCallIterableLeaf(call, calcs[0], calcs[1]);

        if (calcs.length == 2) {
            return tupleIteratorCalc;
        } else {
            Calc[] nextClasls = new Calc[calcs.length - 1];
            nextClasls[0] = tupleIteratorCalc;
            for (int i = 1; i < calcs.length - 1; i++) {
                nextClasls[i] = calcs[i + 1];
            }
            return compileCallIterableArray(call, compiler, nextClasls);
        }
    }

    protected TupleIteratorCalc compileCallIterableLeaf(final ResolvedFunCall call, final Calc calc1,
            final Calc calc2) {
        Calc[] calcs = new Calc[] { calc1, calc2 };
        // The Calcs, 1 and 2, can be of type: Member or Member[] and
        // of ResultStyle: ITERABLE, LIST or MUTABLE_LIST, but
        // LIST and MUTABLE_LIST are treated the same; so
        // there are 16 possible combinations - sweet.

        // Check returned calc ResultStyles
        FunUtil.checkIterListResultStyles(calc1);
        FunUtil.checkIterListResultStyles(calc2);

        return new CrossJoinIterCalc(call, calcs, ctag);
    }

    private Calc toIter(ExpressionCompiler compiler, final Expression exp) {
        // Want iterable, immutable list or mutable list in that order
        // It is assumed that an immutable list is easier to get than
        // a mutable list.
        final Type type = exp.getType();
        if (type instanceof SetType) {
            // this can return an TupleIteratorCalc or TupleListCalc
            return compiler.compile(exp, CompileRequest.as(null, ResultStyle.ITERABLE_LIST_MUTABLELIST));
        } else {
            // this always returns an TupleIteratorCalc
            return new ExprIterCalc(new SetType(type), new Expression[] { exp }, compiler,
                    ResultStyle.ITERABLE_LIST_MUTABLELIST);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Immutable List
    ///////////////////////////////////////////////////////////////////////////

    protected TupleListCalc compileCallImmutableList(final ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        Calc[] calcs = new Calc[args.length];
        for (int i = 0; i < args.length; i++) {
            calcs[i] = toList(compiler, args[i]);
        }
        return compileCallImmutableListArray(call, compiler, calcs);
    }

    protected TupleListCalc compileCallImmutableListArray(final ResolvedFunCall call, ExpressionCompiler compiler,
            Calc[] calcs) {
        TupleListCalc tupleListCalc = compileCallImmutableListLeaf(call, calcs[0], calcs[1]);

        if (calcs.length == 2) {
            return tupleListCalc;
        } else {
            Calc[] nextClasls = new Calc[calcs.length - 1];
            nextClasls[0] = tupleListCalc;
            for (int i = 1; i < calcs.length - 1; i++) {
                nextClasls[i] = calcs[i + 1];
            }
            return compileCallImmutableListArray(call, compiler, nextClasls);
        }
    }

    protected TupleListCalc compileCallImmutableListLeaf(final ResolvedFunCall call, final Calc calc1,
            final Calc calc2) {
        Calc[] calcs = new Calc[] { calc1, calc2 };
        // The Calcs, 1 and 2, can be of type: Member or Member[] and
        // of ResultStyle: LIST or MUTABLE_LIST.
        // Since we want an immutable list as the result, it does not
        // matter whether the Calc list are of type
        // LIST and MUTABLE_LIST - they are treated the same; so
        // there are 4 possible combinations - even sweeter.

        // Check returned calc ResultStyles
        FunUtil.checkListResultStyles(calc1);
        FunUtil.checkListResultStyles(calc2);

        return new ImmutableListCalc(call, calcs, ctag);
    }

    /**
     * Compiles an expression to list (or mutable list) format. Never returns null.
     *
     * @param compiler Compiler
     * @param exp      Expression
     * @return Compiled expression that yields a list or mutable list
     */
    private TupleListCalc toList(ExpressionCompiler compiler, final Expression exp) {
        // Want immutable list or mutable list in that order
        // It is assumed that an immutable list is easier to get than
        // a mutable list.
        final Type type = exp.getType();
        if (type instanceof SetType) {
            final Calc calc = compiler.compile(exp, CompileRequest.as(null, ResultStyle.LIST_MUTABLELIST));
            if (calc == null) {
                return compiler.compileList(exp);
            }
            return (TupleListCalc) calc;
        } else {
            return new SetListCalc(new SetType(type), new Expression[] { exp }, compiler,
                    ResultStyle.LIST_MUTABLELIST);
        }
    }

    protected TupleListCalc compileCallMutableList(final ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        Calc[] calcs = new Calc[args.length];
        for (int i = 0; i < args.length; i++) {
            calcs[i] = toList(compiler, args[i]);
        }
        return compileCallMutableListArray(call, compiler, calcs);
    }

    protected TupleListCalc compileCallMutableListArray(final ResolvedFunCall call, ExpressionCompiler compiler,
            Calc[] calcs) {
        TupleListCalc tupleListCalc = compileCallMutableListLeaf(call, calcs[0], calcs[1]);

        if (calcs.length == 2) {
            return tupleListCalc;
        } else {
            Calc[] nextClasls = new Calc[calcs.length - 1];
            nextClasls[0] = tupleListCalc;
            for (int i = 1; i < calcs.length - 1; i++) {
                nextClasls[i] = calcs[i + 1];
            }
            return compileCallMutableListArray(call, compiler, nextClasls);
        }
    }

    protected TupleListCalc compileCallMutableListLeaf(final ResolvedFunCall call, final Calc calc1, final Calc calc2) {
        Calc[] calcs = new Calc[] { calc1, calc2 };
        // The Calcs, 1 and 2, can be of type: Member or Member[] and
        // of ResultStyle: LIST or MUTABLE_LIST.
        // Since we want an mutable list as the result, it does not
        // matter whether the Calc list are of type
        // LIST and MUTABLE_LIST - they are treated the same,
        // regardless of type, one must materialize the result list; so
        // there are 4 possible combinations - even sweeter.

        // Check returned calc ResultStyles
        FunUtil.checkListResultStyles(calc1);
        FunUtil.checkListResultStyles(calc2);

        return new MutableListCalc(call, calcs, ctag);
    }

    public static TupleList nonEmptyOptimizeList(Evaluator evaluator, TupleList list, ResolvedFunCall call, int ctag) {
        int opSize = evaluator.getCatalogReader().getContext()
                .getConfig().crossJoinOptimizerSize();
        if (list.isEmpty()) {
            return list;
        }

        int size = list.size();

        if (size > opSize && evaluator.isNonEmpty()) {
            // instead of overflow exception try to further
            // optimize nonempty(crossjoin(a,b)) ==
            // nonempty(crossjoin(nonempty(a),nonempty(b))
            final int missCount = evaluator.getMissCount();

            list = nonEmptyList(evaluator, list, call, ctag);
            size = list.size();
            // list may be empty after nonEmpty optimization
            if (size == 0) {
                return TupleCollections.emptyList(list.getArity());
            }
            final int missCount2 = evaluator.getMissCount();
            final int puntMissCountListSize = 1000;
            if (missCount2 > missCount && size > puntMissCountListSize) {
                // We've hit some cells which are not in the cache. They
                // registered as non-empty, but we won't really know until
                // we've populated the cache. The cartesian product is still
                // huge, so let's quit now, and try again after the cache
                // has been loaded.
                // Return an empty list short circuits higher level
                // evaluation poping one all the way to the top.
                return TupleCollections.emptyList(list.getArity());
            }
        }
        return list;
    }

    public static TupleList mutableCrossJoin(TupleList list1, TupleList list2) {
        return TupleCollections.mutableCrossJoin(list1, list2);
    }

    public static TupleList mutableCrossJoin(List<TupleList> lists) {
        return TupleCollections.mutableCrossJoin(lists);
    }

    /**
     * This is the entry point to the crossjoin non-empty optimizer code.
     *
     *
     * What one wants to determine is for each individual Member of the input
     * parameter list, a 'List-Member', whether across a slice there is any data.
     *
     *
     * But what data?
     *
     *
     * For Members other than those in the list, the 'non-List-Members', one wants
     * to consider all data across the scope of these other Members. For instance,
     * if Time is not a List-Member, then one wants to consider data across All
     * Time. Or, if Customer is not a List-Member, then look at data across All
     * Customers. The theory here, is if there is no data for a particular Member of
     * the list where all other Members not part of the list are span their complete
     * hierarchy, then there is certainly no data for Members of that Hierarchy at a
     * more specific Level (more on this below).
     *
     *
     * When a Member that is a non-List-Member is part of a Hierarchy that has an
     * All Member (hasAll="true"), then its very easy to make sure that the All
     * Member is used during the optimization. If a non-List-Member is part of a
     * Hierarchy that does not have an All Member, then one must, in fact, iterate
     * over all top-level Members of the Hierarchy!!! - otherwise a List-Member
     * might be excluded because the optimization code was not looking everywhere.
     *
     *
     * Concerning default Members for those Hierarchies for the non-List-Members,
     * ignore them. What is wanted is either the All Member or one must iterate
     * across all top-level Members, what happens to be the default Member of the
     * Hierarchy is of no relevant.
     *
     *
     * The Measures Hierarchy has special considerations. First, there is no All
     * Measure. But, certainly one need only involve Measures that are actually in
     * the query... yes and no. For Calculated Measures one must also get all of the
     * non-Calculated Measures that make up each Calculated Measure. Thus, one ends
     * up iterating across all Calculated and non-Calculated Measures that are
     * explicitly mentioned in the query as well as all Calculated and
     * non-Calculated Measures that are used to define the Calculated Measures in
     * the query. Why all of these? because this represents the total scope of
     * possible Measures that might yield a non-null value for the List-Members and
     * that is what we what to find. It might be a super set, but thats ok; we just
     * do not want to miss anything.
     *
     *
     * For other Members, the default Member is used, but for Measures one should
     * look for that data for all Measures associated with the query, not just one
     * Measure. For a dense dataset this may not be a problem or even apparent, but
     * for a sparse dataset, the first Measure may, in fact, have not data but other
     * Measures associated with the query might. Hence, the solution here is to
     * identify all Measures associated with the query and then for each Member of
     * the list, determine if there is any data iterating across all Measures until
     * non-null data is found or the end of the Measures is reached.
     *
     *
     * This is a non-optimistic implementation. This means that an element of the
     * input parameter List is only not included in the returned result List if for
     * no combination of Measures, non-All Members (for Hierarchies that have no All
     * Members) and evaluator default Members did the element evaluate to non-null.
     *
     * @param evaluator Evaluator
     * @param list      List of members or tuples
     * @param call      Calling ResolvedFunCall used to determine what Measures to
     *                  use
     * @return List of elements from the input parameter list that have evaluated to
     *         non-null.
     */
    public static TupleList nonEmptyList(Evaluator evaluator, TupleList list, ResolvedFunCall call, int ctag) {
        if (list.isEmpty()) {
            return list;
        }

        TupleList result = TupleCollections.createList(list.getArity(), (list.size() + 2) >> 1);

        // Get all of the Measures
        final Query query = evaluator.getQuery();

        final String measureSetKey = "MEASURE_SET-" + ctag;
        Set<Member> measureSet = Util.cast((Set) query.getEvalCache(measureSetKey));

        final String memberSetKey = "MEMBER_SET-" + ctag;
        Set<Member> memberSet = Util.cast((Set) query.getEvalCache(memberSetKey));
        // If not in query cache, then create and place into cache.
        // This information is used for each iteration so it makes
        // sense to create and cache it.
        if (measureSet == null || memberSet == null) {
            measureSet = new HashSet<>();
            memberSet = new HashSet<>();
            Set<Member> queryMeasureSet = query.getMeasuresMembers();
            MeasureVisitor measureVisitor = new MeasureVisitor(measureSet, call);

            // MemberExtractingVisitor will collect the dimension members
            // referenced within the measures in the query.
            // One or more measures may conflict with the members in the tuple,
            // overriding the context of the tuple member when determining
            // non-emptiness.
            MemberExtractingVisitor memVisitor = new MemberExtractingVisitor(memberSet, call, false);

            for (Member m : queryMeasureSet) {
                if (m.isCalculated()) {
                    Expression exp = m.getExpression();
                    exp.accept(measureVisitor);
                    exp.accept(memVisitor);
                } else {
                    measureSet.add(m);
                }
            }
            Formula[] formula = query.getFormulas();
            if (formula != null) {
                for (Formula f : formula) {
                    if (Util.containsValidMeasure(f.getExpression())) {
                        // short circuit if VM is present.
                        return list;
                    }
                    f.accept(measureVisitor);
                }
            }
            query.putEvalCache(measureSetKey, measureSet);
            query.putEvalCache(memberSetKey, memberSet);
        }

        final String allMemberListKey = "ALL_MEMBER_LIST-" + ctag;
        List<Member> allMemberList = Util.cast((List) query.getEvalCache(allMemberListKey));

        final String nonAllMembersKey = "NON_ALL_MEMBERS-" + ctag;
        Member[][] nonAllMembers = (Member[][]) query.getEvalCache(nonAllMembersKey);
        if (nonAllMembers == null) {
            //
            // Get all of the All Members and those Hierarchies that
            // do not have All Members.
            //
            Member[] evalMembers = evaluator.getMembers().clone();

            List<Member> listMembers = list.get(0);

            // Remove listMembers from evalMembers and independentSlicerMembers
            for (Member lm : listMembers) {
                Hierarchy h = lm.getHierarchy();
                for (int i = 0; i < evalMembers.length; i++) {
                    Member em = evalMembers[i];
                    if ((em != null) && h.equals(em.getHierarchy())) {
                        evalMembers[i] = null;
                    }
                }
            }

            Map<Hierarchy, Set<Member>> mapOfSlicerMembers = evaluator.getSlicerMembersByHierarchy();
            //if (evaluator instanceof RolapEvaluator rev) {
            //    mapOfSlicerMembers = evaluator.getSlicerMembersByHierarchy();
            //}

            // Now we have the non-List-Members, but some of them may not be
            // All Members (default Member need not be the All Member) and
            // for some Hierarchies there may not be an All Member.
            // So we create an array of Objects some elements of which are
            // All Members and others elements will be an array of all top-level
            // Members when there is not an All Member.
            CatalogReader schemaReader = evaluator.getCatalogReader();
            allMemberList = new ArrayList<>();
            List<Member[]> nonAllMemberList = new ArrayList<>();

            Member em;
            boolean isSlicerMember;
            for (Member evalMember : evalMembers) {
                em = evalMember;
                if (em == null) {
                    // Above we might have removed some by setting them
                    // to null. These are the CrossJoin axes.
                    continue;
                }
                if (em.isMeasure()) {
                    continue;
                }

                isSlicerMember = false;
                if (mapOfSlicerMembers != null) {
                    Set<Member> members = mapOfSlicerMembers.get(em.getHierarchy());
                    if (members != null) {
                        isSlicerMember = members.contains(em);
                    }
                }

                //
                // The unconstrained members need to be replaced by the "All"
                // member based on its usage and property. This is currently
                // also the behavior of native cross join evaluation. See
                // SqlConstraintUtils.addContextConstraint()
                //
                // on slicer? | calculated? | replace with All?
                // -----------------------------------------------
                // Y | Y | Y always
                // Y | N | N
                // N | Y | N
                // N | N | Y if not "All"
                // -----------------------------------------------
                //
                if ((isSlicerMember && !em.isCalculated()) || (!isSlicerMember && em.isCalculated())) {
                    // If the slicer contains multiple members from this one's
                    // hierarchy, add them to nonAllMemberList
                    if (isSlicerMember) {
                        Set<Member> hierarchySlicerMembers = mapOfSlicerMembers.get(em.getHierarchy());
                        if (hierarchySlicerMembers.size() > 1) {
                            nonAllMemberList.add(hierarchySlicerMembers.toArray(Member[]::new));
                        }
                    }
                    continue;
                }

                // If the member is not the All member;
                // or if it is a slicer member,
                // replace with the "all" member.
                if (isSlicerMember || !em.isAll()) {
                    Hierarchy h = em.getHierarchy();
                    final List<Member> rootMemberList = schemaReader.getHierarchyRootMembers(h);
                    if (h.hasAll()) {
                        // The Hierarchy has an All member
                        boolean found = false;
                        for (Member m : rootMemberList) {
                            if (m.isAll()) {
                                allMemberList.add(m);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            CrossJoinFunDef.LOGGER.warn("CrossJoinFunDef.nonEmptyListNEW: ERROR");
                        }
                    } else {
                        // The Hierarchy does NOT have an All member
                        Member[] rootMembers = rootMemberList.toArray(Member[]::new);
                        nonAllMemberList.add(rootMembers);
                    }
                }
            }
            nonAllMembers = nonAllMemberList.toArray(Member[][]::new);

            query.putEvalCache(allMemberListKey, allMemberList);
            query.putEvalCache(nonAllMembersKey, nonAllMembers);
        }

        //
        // Determine if there is any data.
        //
        // Put all of the All Members into Evaluator
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setContext(allMemberList);
            // Iterate over elements of the input list. If for any
            // combination of
            // Measure and non-All Members evaluation is non-null, then
            // add it to the result List.
            final TupleCursor cursor = list.tupleCursor();
            int currentIteration = 0;
            Execution execution = query.getStatement().getCurrentExecution();
            while (cursor.forward()) {
                cursor.setContext(evaluator);
                for (Member member : memberSet) {
                    // memberSet contains members referenced within measures.
                    // Make sure that we don't incorrectly assume a context
                    // that will be changed by the measure, so conservatively
                    // push context to [All] for each of the associated
                    // hierarchies.
                    evaluator.setContext(member.getHierarchy().getAllMember());
                }
                // Check if the MDX query was canceled.
                // Throws an exception in case of timeout is exceeded
                // see MONDRIAN-2425
                CancellationChecker.checkCancelOrTimeout(currentIteration++, execution);
                if (tupleContainsCalcs(cursor.current())
                        || CrossJoinFunDef.checkData(nonAllMembers, nonAllMembers.length - 1, measureSet, evaluator)) {
                    result.addCurrent(cursor);
                }
            }
            return result;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private static boolean tupleContainsCalcs(List<Member> current) {
        return current.stream().anyMatch(Member::isCalculated);
    }

    /**
     * Return true if for some combination of Members from the
     * nonAllMembers array of Member arrays and Measures from the Set of Measures
     * evaluate to a non-null value. Even if a particular combination is non-null,
     * all combinations are tested just to make sure that the data is loaded.
     *
     * @param nonAllMembers array of Member arrays of top-level Members for
     *                      Hierarchies that have no All Member.
     * @param cnt           which Member array is to be processed.
     * @param measureSet    Set of all that should be tested against.
     * @param evaluator     the Evaluator.
     * @return True if at least one combination evaluated to non-null.
     */
    private static boolean checkData(Member[][] nonAllMembers, int cnt, Set<Member> measureSet, Evaluator evaluator) {
        if (cnt < 0) {
            // no measures found, use standard algorithm
            if (measureSet.isEmpty()) {
                Object value = evaluator.evaluateCurrent();
                if (value != null && !(value instanceof Throwable)) {
                    return true;
                }
            } else {
                // Here we evaluate across all measures just to
                // make sure that the data is all loaded
                boolean found = false;
                for (Member measure : measureSet) {
                    evaluator.setContext(measure);
                    Object value = evaluator.evaluateCurrent();
                    if (value != null && !(value instanceof Throwable)) {
                        found = true;
                    }
                }
                return found;
            }
        } else {
            boolean found = false;
            for (Member m : nonAllMembers[cnt]) {
                evaluator.setContext(m);
                if (CrossJoinFunDef.checkData(nonAllMembers, cnt - 1, measureSet, evaluator)) {
                    found = true;
                }
            }
            return found;
        }
        return false;
    }

    public int getCtag() {
        return ctag;
    }

}
