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
package org.eclipse.daanse.olap.function.def.rank;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.fun.FunUtil;

public class SortedListCalc extends AbstractProfilingNestedCalc {
    private final TupleListCalc tupleListCalc;

    private static final Integer ONE = 1;

    /**
     * Creates a SortCalc.
     *
     * @param type Type
     * @param tupleListCalc
     *          Compiled expression to compute the list
     * @param keyCalc
     *          Compiled expression to compute the sort key
 */
    public SortedListCalc( Type type, TupleListCalc tupleListCalc, Calc keyCalc ) {
      super( type, tupleListCalc, keyCalc );
      this.tupleListCalc = tupleListCalc;
    }

    @Override
    public boolean dependsOn( Hierarchy hierarchy ) {
      return HierarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
    }

    @Override
    public Object evaluateInternal( Evaluator evaluator ) {
      // Save the state of the evaluator.
      final int savepoint = evaluator.savepoint();
      Calc<?> keyCalc = getChildCalc(1, Calc.class);
      RuntimeException exception = null;
      final Map<Member, Object> memberValueMap;
      final Map<List<Member>, Object> tupleValueMap;
      final int numValues;
      // noinspection unchecked
      final Map<Object, Integer> uniqueValueCounterMap =
          new TreeMap<>( FunUtil.DescendingValueComparator.instance );
      TupleList list;
      try {
        evaluator.setNonEmpty( false );

        // Construct an array containing the value of the expression
        // for each member.

        list = tupleListCalc.evaluate( evaluator );
        assert list != null;
        if ( list.isEmpty() ) {
          return list.getArity() == 1 ? new MemberSortResult( new Object[0], Collections.<Member, Integer>emptyMap() )
              : new TupleSortResult( new Object[0], Collections.<List<Member>, Integer>emptyMap() );
        }

        if ( list.getArity() == 1 ) {
          memberValueMap = new HashMap<>();
          tupleValueMap = null;
          for ( Member member : list.slice( 0 ) ) {
            evaluator.setContext( member );
            final Object keyValue = keyCalc.evaluate( evaluator );
            if ( keyValue instanceof RuntimeException runtimeException ) {
              if ( exception == null ) {
                exception = runtimeException;
              }
            } else if ( Util.isNull( keyValue ) || keyValue == NotLoaded.INSTANCE ) {
              // NULL: nothing to do. NotLoaded: dirty batching pass whose
              // ranks are discarded - skip instead of choking on the
              // non-Comparable marker (the legacy Double(0) sorted silently).
            } else {
              // Assume it's the first time seeing this keyValue.
              Integer valueCounter = uniqueValueCounterMap.put( keyValue, SortedListCalc.ONE );
              if ( valueCounter != null ) {
                // Update the counter on how many times this
                // keyValue has been seen.
                uniqueValueCounterMap.put( keyValue, valueCounter + 1 );
              }
              memberValueMap.put( member, keyValue );
            }
          }
          numValues = memberValueMap.keySet().size();
        } else {
          tupleValueMap = new HashMap<>();
          memberValueMap = null;
          for ( List<Member> tuple : list ) {
            evaluator.setContext( tuple );
            final Object keyValue = keyCalc.evaluate( evaluator );
            if ( keyValue instanceof RuntimeException runtimeException) {
              if ( exception == null ) {
                exception = runtimeException;
              }
            } else if ( Util.isNull( keyValue ) || keyValue == NotLoaded.INSTANCE ) {
              // NULL: nothing to do. NotLoaded: dirty batching pass whose
              // ranks are discarded - skip instead of choking on the
              // non-Comparable marker (the legacy Double(0) sorted silently).
            } else {
              // Assume it's the first time seeing this keyValue.
              Integer valueCounter = uniqueValueCounterMap.put( keyValue, SortedListCalc.ONE );
              if ( valueCounter != null ) {
                // Update the counter on how many times this
                // keyValue has been seen.
                uniqueValueCounterMap.put( keyValue, valueCounter + 1 );
              }
              tupleValueMap.put( tuple, keyValue );
            }
          }
          numValues = tupleValueMap.keySet().size();
        }
      } finally {
        evaluator.restore( savepoint );
      }

      // If there were exceptions, quit now... we'll be back.
      if ( exception != null ) {
        return exception;
      }

      final Object[] allValuesSorted = new Object[numValues];

      // Now build the sorted array containing all keyValues
      // And update the counter to the rank
      int currentOrdinal = 0;
      // noinspection unchecked
      final Map<Object, Integer> uniqueValueRankMap =
          new TreeMap<>( Collections.<Object>reverseOrder() );

      for ( Map.Entry<Object, Integer> entry : uniqueValueCounterMap.entrySet() ) {
        Object keyValue = entry.getKey();
        Integer valueCount = entry.getValue();
        // Because uniqueValueCounterMap is already sorted, so the
        // reconstructed allValuesSorted is guaranteed to be sorted.
        for ( int i = 0; i < valueCount; i++ ) {
          allValuesSorted[currentOrdinal + i] = keyValue;
        }
        uniqueValueRankMap.put( keyValue, currentOrdinal + 1 );
        currentOrdinal += valueCount;
      }

      // Build a member/tuple to rank map
      if ( list.getArity() == 1 ) {
        final Map<Member, Integer> rankMap = new HashMap<>();
        for ( Map.Entry<Member, Object> entry : memberValueMap.entrySet() ) {
          int oneBasedRank = uniqueValueRankMap.get( entry.getValue() );
          rankMap.put( entry.getKey(), oneBasedRank );
        }
        return new MemberSortResult( allValuesSorted, rankMap );
      } else {
        final Map<List<Member>, Integer> rankMap = new HashMap<>();
        if (tupleValueMap != null) {
            for (Map.Entry<List<Member>, Object> entry : tupleValueMap.entrySet()) {
                int oneBasedRank = uniqueValueRankMap.get(entry.getValue());
                rankMap.put(entry.getKey(), oneBasedRank);
            }
        }
        return new TupleSortResult( allValuesSorted, rankMap );
      }
    }
  }
