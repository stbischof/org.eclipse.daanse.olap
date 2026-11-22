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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedIntegerCalc;

public class Rank2MemberCalc extends AbstractNestedIntegerCalc {

    public Rank2MemberCalc( Type type, MemberCalc memberCalc, Calc<?> listCalc ) {
      super( type,  memberCalc, listCalc );
    }

    @Override
    public Integer evaluateInternal(Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {

        // Get member or tuple.
        // If the member is null (or the tuple contains a null member)
        // the result is null (even if the list is null).
        final Member member = getChildCalc(0, MemberCalc.class).evaluate( evaluator );
        if ( member == null || member.isNull() ) {
          return null;
        }
        // Get the set of members/tuples.
        // If the list is empty, MSAS cannot figure out the type of the
        // list, so returns an error "Formula error - dimension count is
        // not valid - in the Rank function". We will naturally return 0,
        // which I think is better.
        RankedMemberList rankedMemberList = (RankedMemberList) getChildCalc(1, Calc.class).evaluate( evaluator );
        if ( rankedMemberList == null ) {
          return 0;
        }

        // Find position of member in list. -1 signifies not found.
        final int i = rankedMemberList.indexOf( member );
        // Return 1-based rank. 0 signifies not found.
        return i + 1;
      } finally {
        evaluator.getTiming().markEnd( RankFunDef.TIMING_NAME );
      }
    }
  }
