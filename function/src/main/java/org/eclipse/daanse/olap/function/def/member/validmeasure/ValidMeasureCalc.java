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
package org.eclipse.daanse.olap.function.def.member.validmeasure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.VirtualCubeMeasure;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.nested.AbstractNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.util.HierarchyDependsChecker;

public class ValidMeasureCalc extends AbstractNestedUnknownCalc {

    private final Calc<?> calc;
    private final static String validMeasureUsingCalculatedMember = """
        The function ValidMeasure cannot be used with the measure ''{0}'' because it is a calculated member. The function should be used to wrap the base measure in the source cube.
    """;

    public ValidMeasureCalc(ResolvedFunCall call, Calc<?> calc) {
        super(call.getType());
        this.calc = calc;
    }

    @Override
    public Object evaluateInternal(Evaluator evaluator) {
        Cube baseCube;
        Cube virtualCube = evaluator.getCube();
        final List<Member> memberList = getCalcsMembers(evaluator);

        if (memberList == null || memberList.size() == 0) {
          // there are no members in the ValidMeasure
          return null;
        }

        if (!virtualCube.drawsFromOtherCubes()) {
            // this is not a virtual cube,
            // there's nothing for ValidMeasure to do.
            // just evaluate sub-expression
            evaluator.setContext(memberList.toArray(
                new Member[memberList.size()]));
            return evaluator.evaluateCurrent();
        }
        // find the measure in the tuple
        int measurePosition = -1;
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getDimension().isMeasures()) {
                measurePosition = i;
                break;
            }
        }

        final Member vcMeasure = memberList.get(measurePosition);

        if (!(vcMeasure instanceof  VirtualCubeMeasure))
        {
            // Cannot use calculated members in ValidMeasure.
            throw new OlapRuntimeException(MessageFormat.format(validMeasureUsingCalculatedMember,vcMeasure.getUniqueName()));
        }

        baseCube = ((VirtualCubeMeasure)vcMeasure).getCube();

        List<Dimension> vMinusBDimensions =
            getDimensionsToForceToAllLevel(
                virtualCube, baseCube, memberList);
        // declare members array and fill in with all needed members
        final List<Member> validMeasureMembers =
            new ArrayList<>(memberList);
        // start adding to validMeasureMembers at right place
        for (Dimension vMinusBDimension : vMinusBDimensions) {
            // add default|all member for each hierarchy
            for (final Hierarchy hierarchy
                : vMinusBDimension.getHierarchies())
            {
                if (hierarchy.hasAll()) {
                    validMeasureMembers.add(hierarchy.getAllMember());
                } else {
                    validMeasureMembers.add(hierarchy.getDefaultMember());
                }
            }
        }
        // this needs to be done before validmeasuremembers are set on the
        // context since calculated members defined on a non joining
        // dimension might have been pulled to default member
        List<Member> calculatedMembers =
            getCalculatedMembersFromContext(evaluator);

        evaluator.setContext(validMeasureMembers);
        evaluator.setContext(calculatedMembers);

        return evaluator.evaluateCurrent();
    }

    private List<Member> getCalcsMembers(Evaluator evaluator) {
        List<Member> memberList;

        MemberCalc mc = null;
        if (calc instanceof MemberCalc tmpMembCalc) {
            mc = tmpMembCalc;
        } else if (calc instanceof MemberCalc tmc) {
            mc = tmc;
        }
        if (mc != null) {
            memberList = Collections.singletonList(mc.evaluate(evaluator));
        } else {
            TupleCalc tc = (TupleCalc) calc;
            final Member[] tupleMembers = tc.evaluate(evaluator);
            if (tupleMembers == null) {
                memberList = null;
            } else {
                memberList = Arrays.asList(tupleMembers);
            }
        }
        return memberList;
    }

    private List<Member> getCalculatedMembersFromContext(
        Evaluator evaluator)
    {
        Member[] currentMembers = evaluator.getMembers();
        List<Member> calculatedMembers = new ArrayList<>();
        for (Member currentMember : currentMembers) {
            if (currentMember.isCalculated()) {
                calculatedMembers.add(currentMember);
            }
        }
        return calculatedMembers;
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[]{calc};
    }

    private List<Dimension> getDimensionsToForceToAllLevel(
        Cube virtualCube,
        Cube baseCube,
        List<Member> memberList)
    {
        List<Dimension> vMinusBDimensions = new ArrayList<>();
        Set<Dimension> virtualCubeDims = new HashSet<>(virtualCube.getDimensions());
        Set<Dimension> nonJoiningDims =
            baseCube.nonJoiningDimensions(virtualCubeDims);

        for (Dimension nonJoiningDim : nonJoiningDims) {
            if (!isDimInMembersList(memberList, nonJoiningDim)) {
                vMinusBDimensions.add(nonJoiningDim);
            }
        }
        return vMinusBDimensions;
    }

    private boolean isDimInMembersList(
        List<Member> members,
        Dimension dimension)
    {
        for (Member member : members) {
            if (member.getName().equalsIgnoreCase(dimension.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        // depends on all hierarchies
        return HierarchyDependsChecker.butDepends(getChildCalcs(), hierarchy);
    }
}
