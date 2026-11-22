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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIterable;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.profile.ProfilingCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;
import org.eclipse.daanse.olap.fun.sort.SortKeySpec;
import org.eclipse.daanse.olap.fun.sort.Sorter;
import org.eclipse.daanse.olap.fun.sort.Sorter.SorterFlag;
import org.eclipse.daanse.olap.calc.base.type.member.MemberOrderKeyCalc;
import org.eclipse.daanse.olap.function.def.order.OrderFunDef.CalcWithDual;

public class OrderCurrentMemberCalc  extends AbstractNestedTupleListCalc implements CalcWithDual {
    private final TupleIteratorCalc<?> tupleIteratorCalc;
    private final Calc<?> sortKeyCalc;
    private final List<SortKeySpec> keySpecList;
    private final int originalKeySpecCount;
    private final int arity;

    public OrderCurrentMemberCalc( Type type, Calc<?>[] calcList, List<SortKeySpec> keySpecList ) {
      super( type, calcList );
      this.tupleIteratorCalc = (TupleIteratorCalc) calcList[0];
      this.sortKeyCalc = calcList[1];
      this.keySpecList = keySpecList;
      this.originalKeySpecCount = keySpecList.size();
      this.arity = getType().getArity();
    }

    @Override
    public TupleList evaluateDual( Evaluator rootEvaluator, Evaluator subEvaluator ) {
      assert originalKeySpecCount == 1;
      final TupleIterable iterable = tupleIteratorCalc.evaluate( rootEvaluator );
      // REVIEW: If iterable happens to be a list, we'd like to pass it,
      // but we cannot yet guarantee that it is mutable.
      // final TupleList list = iterable instanceof ArrayTupleList && false ? (TupleList) iterable : null; old code
      final TupleList list = null;
      tupleIteratorCalc.getResultStyle();
//      discard( tupleIteratorCalc.getResultStyle() );
      return handleSortWithOneKeySpec( subEvaluator, iterable, list );
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator ) {
      evaluator.getTiming().markStart( OrderFunDef.TIMING_NAME );
      try {
        final TupleIterable iterable = tupleIteratorCalc.evaluate( evaluator );
        // REVIEW: If iterable happens to be a list, we'd like to pass it,
        // but we cannot yet guarantee that it is mutable.
        // final TupleList list = iterable instanceof ArrayTupleList && false ? (TupleList) iterable : null; old code list all time null
        final TupleList list = null;
        // go by size of keySpecList before purging
        if ( originalKeySpecCount == 1 ) {
          return handleSortWithOneKeySpec( evaluator, iterable, list );
        } else {
          purgeKeySpecList( keySpecList, list );
          if ( keySpecList.isEmpty() ) {
            return list;
          }
          final TupleList tupleList;
          final int savepoint = evaluator.savepoint();
          try {
            evaluator.setNonEmpty( false );
            if ( arity == 1 ) {
              //tupleList =
              //    new UnaryTupleList( Sorter.sortMembers( evaluator, iterable.slice( 0 ), list == null ? null : list
              //        .slice( 0 ), keySpecList ) ); -- old code list all time null
              tupleList =
                     new UnaryTupleList( Sorter.sortMembers( evaluator, iterable.slice( 0 ), null, keySpecList ) );
            } else {
              tupleList = Sorter.sortTuples( evaluator, iterable, list, keySpecList, arity );
            }
            return tupleList;
          } finally {
            evaluator.restore( savepoint );
          }
        }
      } finally {
        evaluator.getTiming().markEnd( OrderFunDef.TIMING_NAME );
      }
    }

    private TupleList handleSortWithOneKeySpec( Evaluator evaluator, TupleIterable iterable, TupleList list ) {
      SorterFlag sortKeyDir = keySpecList.get( 0 ).getDirection();
      final TupleList tupleList;
      final int savepoint = evaluator.savepoint();
      try {
        evaluator.setNonEmpty( false );
        if ( arity == 1 ) {
          tupleList =
              new UnaryTupleList( Sorter.sortMembers( evaluator, iterable.slice( 0 ), list == null ? null : list.slice(
                  0 ), sortKeyCalc, sortKeyDir.descending, sortKeyDir.brk ) );
        } else {
          tupleList =
              Sorter.sortTuples( evaluator, iterable, list, sortKeyCalc, sortKeyDir.descending, sortKeyDir.brk,
                  arity );
        }
        return tupleList;
      } finally {
        evaluator.restore( savepoint );
      }
    }

    @Override
    protected Map<String, Object> profilingProperties(Map<String, Object> properties) {


        StringBuilder result = new StringBuilder();
        for (SortKeySpec spec : keySpecList) {
            if (result.length() > 0) {
                result.append(",");
            }

            SorterFlag sortKeyDir = spec.getDirection();
            result.append(sortKeyDir.descending ? getDesc(sortKeyDir.brk) : getAsc(sortKeyDir.brk));
        }
        properties.put("direction", result.toString());

        return super.profilingProperties(properties);
    }


    @Override
    public boolean dependsOn( Hierarchy hierarchy ) {
      return HierarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
    }

    private SorterFlag getDesc(boolean brk) {
        return brk ? SorterFlag.BDESC : SorterFlag.DESC;
    }

    private SorterFlag getAsc(boolean brk) {
        return brk ? SorterFlag.BASC : SorterFlag.ASC;
    }

      private void purgeKeySpecList( List<SortKeySpec> keySpecList, TupleList list ) {
      if ( list == null || list.isEmpty() ) {
        return;
      }
      if ( keySpecList.size() == 1 ) {
        return;
      }
      List<Hierarchy> listHierarchies = new ArrayList<>( list.getArity() );
      for ( Member member : list.get( 0 ) ) {
        listHierarchies.add( member.getHierarchy() );
      }
      // do not sort (remove sort key spec from the list) if
      // 1. <member_value_expression> evaluates to a member from a
      // level/dimension which is not used in the first argument
      // 2. <member_value_expression> evaluates to the same member for
      // all cells; for example, a report showing all quarters of
      // year 1998 will not be sorted if the sort key is on the constant
      // member [1998].[Q1]
      ListIterator<SortKeySpec> iter = keySpecList.listIterator();
      while ( iter.hasNext() ) {
        SortKeySpec key = iter.next();
        Calc<?> expCalc = key.getKey();
        if ( ProfilingCalc.unwrap(expCalc) instanceof MemberOrderKeyCalc calc) {
          Calc<?>[] calcs = calc.getChildCalcs();
          MemberCalc memberCalc = (MemberCalc) calcs[0];
          if ( memberCalc instanceof org.eclipse.daanse.olap.api.calc.ConstantCalc || !listHierarchies.contains( memberCalc.getType()
              .getHierarchy() ) ) {
            iter.remove();
          }
        }
      }
    }
  }
