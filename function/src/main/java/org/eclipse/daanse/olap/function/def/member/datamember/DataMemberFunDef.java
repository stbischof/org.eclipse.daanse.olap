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
package org.eclipse.daanse.olap.function.def.member.datamember;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;

public class DataMemberFunDef extends AbstractFunctionDefinition {
    static OperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("DataMember");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom,
            "Returns the system-generated data member that is associated with a nonleaf member of a dimension.",
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.MEMBER).describedAs("Member") })
            .interfaceName(FunctionInterface.NAVIGATION).withTextKey("Member.DataMember.Property").caption("Member.DataMember Property");

    public DataMemberFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        return new AbstractNestedMemberCalc(call.getType(), new Calc[] { memberCalc }) {
            @Override
            public Member evaluateInternal(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                return member.getDataMember();
            }
        };
    }
}
