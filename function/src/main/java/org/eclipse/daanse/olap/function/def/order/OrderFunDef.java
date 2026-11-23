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

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ConstantCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.tuple.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.AbstractNestedCalc;
import org.eclipse.daanse.olap.calc.base.profile.ProfilingCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.MemberArrayValueCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.MemberValueCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.fun.sort.SortKeySpec;
import org.eclipse.daanse.olap.fun.sort.Sorter.SorterFlag;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class OrderFunDef  extends AbstractFunctionDefinition {
    static OperationAtom functionAtom = new FunctionOperationAtom("Order");
    static final String TIMING_NAME = OrderFunDef.class.getSimpleName();
    public static final String CALC_IMPL = "CurrentMemberCalc";

    public OrderFunDef( FunctionParameterR[] argTypes ) {
    super( new FunctionMetaDataR(functionAtom,
            "Arranges members of a set, optionally preserving or breaking the hierarchy.",
            DataType.SET, argTypes).withTextKey("Order.Function").caption("Order Function"));
  }

  @Override
public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final TupleIteratorCalc listCalc = compiler.compileIterable(call.getArg( 0 ));
    List<SortKeySpec> keySpecList = new ArrayList<>();
    buildKeySpecList( keySpecList, call, compiler );
    final int keySpecCount = keySpecList.size();
    Calc<?>[] calcList = new Calc[keySpecCount + 1]; // +1 for the listCalc
    calcList[0] = listCalc;

    assert keySpecCount >= 1;
    final Calc<?> expCalc = keySpecList.get( 0 ).getKey();
    calcList[1] = expCalc;
    // the calc itself, not the hull a profiled statement puts around it
    final Calc<?> bareExpCalc = ProfilingCalc.unwrap( expCalc );

    if (keySpecCount == 1 && ( bareExpCalc instanceof MemberValueCalc ) ||( bareExpCalc instanceof MemberArrayValueCalc ) ) {
        List<MemberCalc> constantList = new ArrayList<>();
        List<MemberCalc> variableList = new ArrayList<>();
        final MemberCalc[] calcs = (MemberCalc[]) ( (AbstractNestedCalc) bareExpCalc ).getChildCalcs();
        for ( MemberCalc memberCalc : calcs ) {
            if ((memberCalc instanceof ConstantCalc) && !listCalc.dependsOn(memberCalc.getType().getHierarchy())) {
            constantList.add( memberCalc );
          } else {
            variableList.add( memberCalc );
          }
        }
        if ( constantList.isEmpty() ) {
          // All members are non-constant -- cannot optimize
        } else if ( variableList.isEmpty() ) {
          // All members are constant. Optimize by setting entire
          // context first.
          calcList[1] = new CurrentValueUnknownCalc( expCalc.getType()  );
          return new OrderContextCalc( calcs, new OrderCurrentMemberCalc(call.getType(), calcList, keySpecList ) );
        } else {
          // Some members are constant. Evaluate these before
          // evaluating the list expression.
          calcList[1] =
              MemberValueCalc.create(  expCalc.getType() , variableList.toArray(
                  new MemberCalc[variableList.size()] ), compiler.getEvaluator()
                      .mightReturnNullForUnrelatedDimension() );
          return new OrderContextCalc( constantList.toArray( new MemberCalc[constantList.size()] ), new OrderCurrentMemberCalc( call.getType(),
              calcList, keySpecList ) );
        }
    }
    for ( int i = 1; i < keySpecCount; i++ ) {
      final Calc<?> expCalcs = keySpecList.get( i ).getKey();
      calcList[i + 1] = expCalcs;
    }
    return new OrderCurrentMemberCalc( call.getType(), calcList, keySpecList );
  }

  private void buildKeySpecList( List<SortKeySpec> keySpecList, ResolvedFunCall call, ExpressionCompiler compiler ) {
    final int argCount = call.getArgs().length;
    int j = 1; // args[0] is the input set
    Calc<?> key;
    SorterFlag dir;
    Expression arg;
    while ( j < argCount ) {
      arg = call.getArg( j );
      key = compiler.compileExactScalar(arg);
      j++;
      if ( ( j >= argCount ) || ( call.getArg( j ).getCategory() != DataType.SYMBOL) ) {
        dir = SorterFlag.ASC;
      } else {
        dir = FunUtil.getLiteralArg( call, j, SorterFlag.ASC, SorterFlag.class );
        j++;
      }
      keySpecList.add( new SortKeySpec( key, dir ) );
    }
  }

  public interface CalcWithDual extends TupleListCalc {
    public TupleList evaluateDual( Evaluator rootEvaluator, Evaluator subEvaluator );
  }
}
