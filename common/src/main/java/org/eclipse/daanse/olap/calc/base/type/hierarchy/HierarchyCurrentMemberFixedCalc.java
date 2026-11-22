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
package org.eclipse.daanse.olap.calc.base.type.hierarchy;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;

public class HierarchyCurrentMemberFixedCalc extends AbstractNestedMemberCalc {
    // getContext works faster if we give RolapHierarchy rather than
    // Hierarchy
    private final Hierarchy hierarchy;

    public HierarchyCurrentMemberFixedCalc(Type type, Hierarchy hierarchy) {
        super(type, new Calc[] {});
        assert hierarchy != null;
        this.hierarchy = hierarchy;
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        HierarchyCurrentMemberCalc.validateSlicerMembers(hierarchy, evaluator);
        return evaluator.getContext(hierarchy);
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return this.hierarchy == hierarchy;
    }
}
