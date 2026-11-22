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
package org.eclipse.daanse.olap.function.def.caption.member;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedStringCalc;

public class CaptionCalc extends AbstractNestedStringCalc {

    protected CaptionCalc(Type type, final MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    @Override
    public String evaluateInternal(Evaluator evaluator) {
        final Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        return member.getCaption();
    }

}
