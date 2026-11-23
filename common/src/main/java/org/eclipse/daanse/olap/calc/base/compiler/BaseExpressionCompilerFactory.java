/*
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
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

package org.eclipse.daanse.olap.calc.base.compiler;

import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.query.Validator;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope = ServiceScope.SINGLETON, configurationPid = BaseExpressionCompilerFactory.PID, service = ExpressionCompilerFactory.class)
public class BaseExpressionCompilerFactory implements ExpressionCompilerFactory {
    public static final String PID = "daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory";

    public BaseExpressionCompilerFactory() {
        this(Map.of());
    }

    @Activate
    public BaseExpressionCompilerFactory(Map<String, Object> map) {
    }

    @Override
    public ExpressionCompiler createExpressionCompiler(Evaluator evaluator, Validator validator,
            List<ResultStyle> resultStyles, List<org.eclipse.daanse.olap.api.calc.compiler.CompileHook> hooks) {
        return new StandardExpressionCompiler(evaluator, validator, resultStyles, hooks);
    }

}