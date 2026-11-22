/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.kpi;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.element.KPI;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;
import org.eclipse.daanse.olap.fun.FunUtil;

public class KPITrendCalc  extends AbstractNestedMemberCalc {

    protected KPITrendCalc(Type type, StringCalc kpiCalc) {
        super(type, kpiCalc);
    }

    @Override
    protected Member evaluateInternal(Evaluator evaluator) {
        final String kpi = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        List<? extends KPI> kpis  = evaluator.getCube().getKPIs();
        Optional<? extends KPI> oKpi = kpis.stream().filter(k -> k.getName().equals(kpi)).findAny();
        if (oKpi.isPresent()) {
            String trend = oKpi.get().getTrend();
            Member member = FunUtil.parseMember(evaluator, trend, null);
            return member;
        } else {
            throw FunUtil.newEvalException(
                    new OlapRuntimeException("Trend is absent for " + kpi));
        }
    }

}
