/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.function.def.iif;

import org.eclipse.daanse.olap.api.calc.compiler.CompileRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.function.def.set.SetListCalc;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IifFunDefTest {

  private Expression logicalParamMock = mock( Expression.class );
  private Expression trueCaseParamMock = mock( Expression.class );
  private Expression falseCaseParamMock = mock( Expression.class );
  private FunctionDefinition funDefMock = mock( FunctionDefinition.class );
  private ExpressionCompiler compilerMock = mock( ExpressionCompiler.class );
  private Expression[] args = new Expression[] { logicalParamMock, trueCaseParamMock, falseCaseParamMock };
  private SetType setTypeMock = mock( SetType.class );
  private SetListCalc setListCalc;
  ResolvedFunCallImpl call;

  @BeforeEach
  protected void setUp() throws Exception {
    when( trueCaseParamMock.getType() ).thenReturn( setTypeMock );
    setListCalc = new SetListCalc( setTypeMock, new Expression[] { args[1] }, compilerMock, ResultStyle.LIST_MUTABLELIST );
    call = new ResolvedFunCallImpl( funDefMock, args, setTypeMock );
    doReturn( setListCalc ).when( compilerMock ).compile(any(), any(CompileRequest.class));
  }

  @Test
  void getResultType() {
    ResultStyle actualResStyle = null;
    ResultStyle expectedResStyle = setListCalc.getResultStyle();
    // Compile calculation for IIf function for (<Logical Expression>, <SetType>, <SetType>) params
    IifSetResolver resolver = new IifSetResolver();
    Calc calc = new IifFunDef(resolver.getRepresentativeFunctionMetaDatas().get(0)).compileCall( call, compilerMock );
    try {
      actualResStyle = calc.getResultStyle();
    } catch ( Exception e ) {
      org.junit.jupiter.api.Assertions.fail( "Should not have thrown any exception." );
    }
    org.junit.jupiter.api.Assertions.assertNotNull( actualResStyle );
    org.junit.jupiter.api.Assertions.assertEquals( expectedResStyle, actualResStyle );

  }

}
