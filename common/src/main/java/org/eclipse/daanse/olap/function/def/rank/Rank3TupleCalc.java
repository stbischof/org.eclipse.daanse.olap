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

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.daanse.olap.api.cache.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class Rank3TupleCalc extends AbstractProfilingNestedIntegerCalc {
    private final ExpCacheDescriptor cacheDescriptor;

    public Rank3TupleCalc( Type type, TupleCalc tupleCalc, Calc<?> sortCalc,
        ExpCacheDescriptor cacheDescriptor ) {
      super( type, tupleCalc, sortCalc );
      this.cacheDescriptor = cacheDescriptor;
    }

    @Override
    public Integer evaluateInternal( Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {
        Member[] members = getChildCalc(0, TupleCalc.class).evaluate( evaluator );
        if ( members == null ) {
          return null;
        }
        assert !FunUtil.tupleContainsNullMember( members );

        // Evaluate the list (or retrieve from cache).
        // If there is an exception while calculating the
        // list, propagate it up.
        final SortResult sortResult = (SortResult) evaluator.getCachedResult( cacheDescriptor );
        if ( RankFunDef.DEBUG) {
          sortResult.log();
        }

        if ( sortResult.isEmpty() ) {
          // If list is empty, the rank is null.
          return null;
        }

        // First try to find the member in the cached SortResult
        Integer rank = null;
        if (sortResult instanceof TupleSortResult tupleSortResult) {
          rank = tupleSortResult.rankOf(members);
        }
        if (sortResult instanceof MemberSortResult memberSortResult && members.length > 0) {
            rank = memberSortResult.rankOf(members[0]);
        }
        if ( rank != null ) {
          return rank;
        }

        // member is not seen before, now compute the value of the tuple.
        final int savepoint = evaluator.savepoint();
        Object value;
        try {
          evaluator.setContext( members );
          value = getChildCalc(1, Calc.class).evaluate( evaluator );
        } finally {
          evaluator.restore( savepoint );
        }

        if ( RankFunDef.valueNotReady( value ) ) {
          // The value wasn't ready, so quit now... we'll be back.
          return 0;
        }

        // If value is null, it won't be in the values array.
        if ( NullSemantics.isNull(value) ) {
          return sortResult.values.length + 1;
        }

        value = RankFunDef.coerceValue( sortResult.values, value );

        // Look for the ranked value in the array.
        int j = Arrays.binarySearch( sortResult.values, value, Collections.<Object>reverseOrder() );
        if ( j < 0 ) {
          // Value not found. Flip the result to find the
          // insertion point.
          j = -( j + 1 );
        }
        return j + 1; // 1-based
      } finally {
        evaluator.getTiming().markEnd( RankFunDef.TIMING_NAME );
      }
    }
  }
