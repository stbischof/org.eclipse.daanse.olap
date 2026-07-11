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
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.NullSemantics;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

public class Rank3MemberCalc extends AbstractProfilingNestedIntegerCalc {
    private final ExpCacheDescriptor cacheDescriptor;

    public Rank3MemberCalc( Type type, MemberCalc memberCalc, Calc<?> sortCalc,
        ExpCacheDescriptor cacheDescriptor ) {
      super( type, memberCalc, sortCalc );
      this.cacheDescriptor = cacheDescriptor;
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {
        Member member = getChildCalc(0, MemberCalc.class).evaluate( evaluator );
        if ( member == null || member.isNull() ) {
          return null;
        }

        // Evaluate the list (or retrieve from cache).
        // If there was an exception while calculating the
        // list, propagate it up.
        final MemberSortResult sortResult = (MemberSortResult) evaluator.getCachedResult( cacheDescriptor );
        if ( RankFunDef.DEBUG) {
          sortResult.log();
        }
        if ( sortResult.isEmpty() ) {
          // If list is empty, the rank is null.
          return null;
        }

        // First try to find the member in the cached SortResult
        Integer rank = sortResult.rankOf( member );
        if ( rank != null ) {
          return rank;
        }

        // member is not seen before, now compute the value of the tuple.
        final int savepoint = evaluator.savepoint();
        evaluator.setContext( member );
        Object value;
        try {
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
