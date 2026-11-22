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
package org.eclipse.daanse.olap.function.def.vba.val;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedDoubleCalc;

public class ValCalc extends AbstractNestedDoubleCalc {

    protected ValCalc(Type type, Calc<?> doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Double evaluateInternal(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        // The Val function stops reading the string at the first character it
        // can't recognize as part of a number. Symbols and characters that are
        // often considered parts of numeric values, such as dollar signs and
        // commas, are not recognized. However, the function recognizes the
        // radix prefixes &O (for octal) and &H (for hexadecimal). Blanks,
        // tabs, and linefeed characters are stripped from the argument.
        //
        // The following returns the value 1615198:
        //
        // Val(" 1615 198th Street N.E.")
        // In the code below, Val returns the decimal value -1 for the
        // hexadecimal value shown:
        //
        // Val("&HFFFF")
        // Note The Val function recognizes only the period (.) as a valid
        // decimal separator. When different decimal separators are used, as in
        // international applications, use CDbl instead to convert a string to
        // a number.

        string = string.replaceAll("\\s", ""); // remove all whitespace
        if (string.startsWith("&H")) {
            string = string.substring(2);
            Pattern p = Pattern.compile("[0-9a-fA-F]*");
            Matcher m = p.matcher(string);
            m.find();
            return (double) Integer.parseInt(m.group(), 16);
        } else if (string.startsWith("&O")) {
            string = string.substring(2);
            Pattern p = Pattern.compile("[0-7]*");
            Matcher m = p.matcher(string);
            m.find();
            return (double) Integer.parseInt(m.group(), 8);
        } else {
            // find the first number
            Pattern p = Pattern.compile("-?[0-9]*[.]?[0-9]*");
            Matcher m = p.matcher(string);
            m.find();
            return Double.parseDouble(m.group());
        }
    }

}
