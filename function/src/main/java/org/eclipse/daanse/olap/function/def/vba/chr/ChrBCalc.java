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
package org.eclipse.daanse.olap.function.def.vba.chr;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class ChrBCalc extends AbstractNestedStringCalc {

    protected ChrBCalc(Type type, final IntegerCalc charcodeCalc) {
        super(type, charcodeCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        Integer charCode = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        return new String(new byte[] { (byte) (int)charCode });
    }

}
