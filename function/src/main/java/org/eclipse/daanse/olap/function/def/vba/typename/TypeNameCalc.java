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
package org.eclipse.daanse.olap.function.def.vba.typename;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class TypeNameCalc extends AbstractNestedStringCalc {

    protected TypeNameCalc(Type type, final Calc<?> varNameCalc) {
        super(type, varNameCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Object varName = getChildCalc(0, Calc.class).evaluate(evaluator);
        // The string returned by TypeName can be any one of the following:
        //
        // String returned Variable
        // object type An object whose type is objecttype
        // Byte Byte value
        // Integer Integer
        // Long Long integer
        // Single Single-precision floating-point number
        // Double Double-precision floating-point number
        // Currency Currency value
        // Decimal Decimal value
        // Date Date value
        // String String
        // Boolean Boolean value
        // Error An error value
        // Empty Uninitialized
        // Null No valid data
        // Object An object
        // Unknown An object whose type is unknown
        // Nothing Object variable that doesn't refer to an object

        if (varName == null) {
            return "NULL";
        } else {
            // strip off the package information
            String name = varName.getClass().getName();
            if (name.lastIndexOf(".") >= 0) {
                name = name.substring(name.lastIndexOf(".") + 1);
            }
            return name;
        }

    }

}
