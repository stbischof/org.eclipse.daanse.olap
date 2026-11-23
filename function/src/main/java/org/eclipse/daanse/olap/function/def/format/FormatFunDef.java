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
package org.eclipse.daanse.olap.function.def.format;

import java.util.Locale;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.util.format.Format;

public class FormatFunDef extends AbstractFunctionDefinition {

        public FormatFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData);
        }

        @Override
        public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final Expression[] args = call.getArgs();
            final Calc<?> calc = compiler.compileExactScalar(call.getArg(0));
            final Locale locale = compiler.getEvaluator().getConnectionLocale();
            if (args[1] instanceof Literal) {
                // Constant string expression: optimize by
                // compiling format string.
                String formatString = (String) ((Literal<?>) args[1]).getValue();
                final Format format = new Format(formatString, locale);
                return new FormatLiteralCalc(call.getType(), calc, format);
            } else {
                // Variable string expression
                final StringCalc stringCalc =
                        compiler.compileString(call.getArg(1));
                return new FormatCalc(call.getType(), calc, stringCalc, locale) {
                };
            }
        }
    }
