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

import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedMemberCalc;
import org.eclipse.daanse.olap.exceptions.CurrentMemberWithCompoundSlicerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HierarchyCurrentMemberCalc extends AbstractNestedMemberCalc {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchyCurrentMemberCalc.class);
    private final HierarchyCalc hierarchyCalc;

    public HierarchyCurrentMemberCalc(Type type, HierarchyCalc hierarchyCalc) {
        super(type, hierarchyCalc);
        this.hierarchyCalc = hierarchyCalc;
    }

    @Override
    public Member evaluateInternal(Evaluator evaluator) {
        Hierarchy hierarchy = hierarchyCalc.evaluate(evaluator);
        validateSlicerMembers(hierarchy, evaluator);
        return evaluator.getContext(hierarchy);
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return hierarchyCalc.getType().usesHierarchy(hierarchy, false);
    }

    public static void validateSlicerMembers(Hierarchy hierarchy, Evaluator evaluator) {
        //if (evaluator instanceof RolapEvaluator rev) {

            String alertValue = evaluator.getCatalogReader().getContext()
                    .getConfig().currentMemberWithCompoundSlicerAlert();

            if (alertValue.equalsIgnoreCase("OFF")) {
                return; // No validation
            }

            Map<Hierarchy, Set<Member>> map = evaluator.getSlicerMembersByHierarchy();
            Set<Member> members = map.get(hierarchy);

            if (members != null && members.size() > 1) {
                OlapRuntimeException exception = new CurrentMemberWithCompoundSlicerException(hierarchy.getUniqueName());

                if (alertValue.equalsIgnoreCase("WARN")) {
                    LOGGER.warn(exception.getMessage());
                } else if (alertValue.equalsIgnoreCase("ERROR")) {
                    throw new CurrentMemberWithCompoundSlicerException(hierarchy.getUniqueName());
                }
            }
        //}
    }

}
