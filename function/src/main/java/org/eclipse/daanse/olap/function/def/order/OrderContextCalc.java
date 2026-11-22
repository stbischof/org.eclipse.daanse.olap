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
package org.eclipse.daanse.olap.function.def.order;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.order.OrderFunDef.CalcWithDual;

public class OrderContextCalc extends AbstractNestedTupleListCalc {
    private final MemberCalc[] memberCalcs;
    private final CalcWithDual calc;
    private final Member[] members; // workspace

    protected OrderContextCalc( MemberCalc[] memberCalcs, CalcWithDual calc ) {
      super( calc.getType() , OrderContextCalc.xx( memberCalcs, calc ) );
      this.memberCalcs = memberCalcs;
      this.calc = calc;
      this.members = new Member[memberCalcs.length];
    }

    private static Calc<?>[] xx( MemberCalc[] memberCalcs, CalcWithDual calc ) {
      Calc<?>[] calcs = new Calc[memberCalcs.length + 1];
      System.arraycopy( memberCalcs, 0, calcs, 0, memberCalcs.length );
      calcs[calcs.length - 1] = calc;
      return calcs;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator ) {
      // Evaluate each of the members, and set as context in the
      // sub-evaluator.
      for ( int i = 0; i < memberCalcs.length; i++ ) {
        members[i] = memberCalcs[i].evaluate( evaluator );
      }
      Evaluator subEval=evaluator.push();
      subEval.setContext(members);
      // Evaluate the expression in the new context.
      return calc.evaluateDual( evaluator, subEval );
    }

    @Override
    public boolean dependsOn( Hierarchy hierarchy ) {
      if ( HierarchyDependsChecker.checkAnyDependsOnChildren(  memberCalcs,hierarchy ) ) {
        return true;
      }
      // Member calculations generate members, which mask the actual
      // expression from the inherited context.
      for ( MemberCalc memberCalc : memberCalcs ) {
        if ( memberCalc.getType().usesHierarchy( hierarchy, true ) ) {
          return false;
        }
      }
      return calc.dependsOn( hierarchy );
    }

    @Override
    public ResultStyle getResultStyle() {
      return calc.getResultStyle();
    }
  }
