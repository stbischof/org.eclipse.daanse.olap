/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.vba.replace;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class ReplaceCalc extends AbstractNestedStringCalc {
    protected ReplaceCalc(Type type, final StringCalc expressionCalc, final StringCalc findCalc,
            final StringCalc replaceCalc, final IntegerCalc startCalc, final IntegerCalc countCalc,
            final IntegerCalc compareCalc) {
        super(type, expressionCalc, findCalc, replaceCalc, startCalc, countCalc, compareCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        String expression = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        String find = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        String replace = getChildCalc(2, StringCalc.class).evaluate(evaluator);
        Integer start = getChildCalc(3, IntegerCalc.class).evaluate(evaluator);
        Integer count = getChildCalc(4, IntegerCalc.class).evaluate(evaluator);
        Integer compare = getChildCalc(5, IntegerCalc.class).evaluate(evaluator); // compare is currently ignored
        return replace(expression, find, replace, start, count);
    }

    public static String replace(String expression, String find, String repl, int start, int count) {
        if (expression == null || find == null || repl == null) return expression;
        if (find.isEmpty()) return expression;                // avoid infinite loop semantics
        if (count == 0) return expression;

        final int from = Math.max(0, start - 1);              // 1-based -> 0-based, clamp at 0
        if (from >= expression.length()) return expression;   // nothing to do

        final String s = expression;
        final StringBuilder out = new StringBuilder(s.length());
        out.append(s, 0, from);

        int pos = from;
        int replaced = 0;
        final boolean unlimited = (count < 0);

        for (;;) {
            int idx = s.indexOf(find, pos);
            if (idx < 0) break;
            if (!unlimited && replaced == count) break;

            out.append(s, pos, idx);
            out.append(repl);
            pos = idx + find.length();
            replaced++;
        }

        out.append(s, pos, s.length());
        return out.toString();
    }

}
