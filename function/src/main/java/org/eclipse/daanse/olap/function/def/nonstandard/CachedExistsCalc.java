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
package org.eclipse.daanse.olap.function.def.nonstandard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;

public class CachedExistsCalc extends AbstractNestedTupleListCalc {

    private static final String TIMING_NAME = CachedExistsFunDef.class.getSimpleName();

    protected CachedExistsCalc(Type type, final TupleListCalc listCalc, final TupleCalc tupleCalc,
            final StringCalc stringCalc) {
        super(type, listCalc, tupleCalc, stringCalc);
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator) {
        evaluator.getTiming().markStart(TIMING_NAME);
        try {

            Member[] subtotal = getChildCalc(1, TupleCalc.class).evaluate(evaluator);
            String namedSetName = getChildCalc(2, StringCalc.class).evaluate(evaluator);

            Object cacheObj = evaluator.getQuery().getEvalCache(makeSetCacheKey(namedSetName, subtotal));
            if (cacheObj != null) {
                HashMap<String, TupleList> setCache = (HashMap<String, TupleList>) cacheObj;
                TupleList tuples = setCache.get(makeSubtotalKey(subtotal));
                if (tuples == null) {
                    tuples = TupleCollections.emptyList(getChildCalc(0, TupleListCalc.class).getType().getArity());
                }
                return tuples;
            }

            // Build a mapping from subtotal tuple types to the input set's tuple types
            List<Hierarchy> listHiers = getHierarchies(getChildCalc(0, TupleListCalc.class).getType());
            List<Hierarchy> subtotalHiers = getHierarchies(getChildCalc(1, TupleCalc.class).getType());
            int[] subtotalToListIndex = new int[subtotalHiers.size()];
            for (int i = 0; i < subtotalToListIndex.length; i++) {
                Hierarchy subtotalHier = subtotalHiers.get(i);
                boolean found = false;
                for (int j = 0; j < listHiers.size(); j++) {
                    if (listHiers.get(j) == subtotalHier) {
                        subtotalToListIndex[i] = j;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Hierarchy in <Tuple> not present in <Set>");
                }
            }

            // Build subtotal cache
            HashMap<String, TupleList> setCache = new HashMap<>();
            TupleList setToCache = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
            for (List<Member> tuple : setToCache) {
                String subtotalKey = makeSubtotalKey(subtotalToListIndex, tuple, subtotal);
                TupleList tupleCache = setCache.get(subtotalKey);
                if (tupleCache == null) {
                    tupleCache = TupleCollections.createList(getChildCalc(0, TupleListCalc.class).getType().getArity());
                    setCache.put(subtotalKey, tupleCache);
                }
                tupleCache.add(tuple);
            }
            evaluator.getQuery().putEvalCache(makeSetCacheKey(namedSetName, subtotal), setCache);

            TupleList tuples = setCache.get(makeSubtotalKey(subtotal));
            if (tuples == null) {
                tuples = TupleCollections.emptyList(getChildCalc(0, TupleListCalc.class).getType().getArity());
            }
            return tuples;
        } finally {
            evaluator.getTiming().markEnd(TIMING_NAME);
        }
    }

    /**
     * Returns a list of hierarchies used by the input type.
     *
     * If an input type is a dimension instead of a hierarchy, then return the
     * dimension's default hierarchy. See MONDRIAN-2704
     *
     * @param t
     * @return
     */
    private List<Hierarchy> getHierarchies(Type t) {
        List<Hierarchy> hiers = new ArrayList<>();
        if (t instanceof MemberType) {
            hiers.add(getHierarchy(t));
        } else if (t instanceof TupleType tupleType) {
            for (Type elementType : tupleType.elementTypes) {
                hiers.add(getHierarchy(elementType));
            }
        } else if (t instanceof SetType setType) {
            if (setType.getElementType() instanceof MemberType) {
                hiers.add(getHierarchy(setType.getElementType()));
            } else if (setType.getElementType() instanceof TupleType tupleTypes) {
                for (Type elementType : tupleTypes.elementTypes) {
                    hiers.add(getHierarchy(elementType));
                }
            }
        }
        return hiers;
    }

    private Hierarchy getHierarchy(Type t) {
        if (t.getHierarchy() != null) {
            return t.getHierarchy();
        }
        return t.getDimension().getHierarchy();
    }

    /**
     * Generates a subtotal key for the input tuple based on the type of the input
     * subtotal tuple.
     *
     * For example, if the subtotal tuple contained:
     *
     * ([Product].[Food], [Time].[1998])
     *
     * then the type of this subtotal tuple is:
     *
     * ([Product].[Family], [Time].[Year])
     *
     * The subtotal key would need to contain the same types from the input tuple.
     *
     * So if a sample input tuple contained:
     *
     * ([Gender].[M], [Product].[Drink].[Dairy], [Time].[1997].[Q1])
     *
     * The subtotal key for this tuple would be:
     *
     * ([Product].[Drink], [Time].[1997])
     *
     *
     * @param subtotalToListIndex
     * @param tuple
     * @param subtotal
     * @return
     */
    private String makeSubtotalKey(int[] subtotalToListIndex, List<Member> tuple, Member[] subtotal) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < subtotal.length; i++) {
            Member subtotalMember = subtotal[i];
            Member tupleMember = tuple.get(subtotalToListIndex[i]);
            int parentLevels = tupleMember.getDepth() - subtotalMember.getDepth();
            while (parentLevels-- > 0) {
                tupleMember = tupleMember.getParentMember();
            }
            builder.append(tupleMember.getUniqueName());
        }
        return builder.toString();
    }

    private String makeSetCacheKey(String setName, Member[] members) {
        StringBuilder builder = new StringBuilder();
        builder.append(setName);
        for (Member m : members) {
            builder.append(m.getLevel().getUniqueName());
        }
        return builder.toString();
    }

    private String makeSubtotalKey(Member[] members) {
        StringBuilder builder = new StringBuilder();
        for (Member m : members) {
            builder.append(m.getUniqueName());
        }
        return builder.toString();
    }

}
