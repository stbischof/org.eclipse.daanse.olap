/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.function.def.member.datamember;

import org.eclipse.daanse.olap.testkit.function.FunctionContract;
import static org.eclipse.daanse.olap.api.DataType.DIMENSION;
import static org.eclipse.daanse.olap.api.DataType.HIERARCHY;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.testkit.function.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code DataMember}: {@code <Member>.DataMember} — the
 * system-generated member holding the raw fact data associated with a non-leaf member. {@code
 * DataMemberFunDef} is resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver}
 * over a single {@code MEMBER} parameter — {@code resolve()} delegates entirely to the generic
 * {@code FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could
 * diverge from the declared signature or throw instead of returning empty.
 *
 * <p>{@code DataMemberFunDef.compileCall} builds its own anonymous {@code
 * AbstractNestedMemberCalc} inline, duplicating {@code member.getDataMember()} — the
 * sibling {@code DataMemberCalc} class in the same package does exactly the same thing but is
 * never actually instantiated anywhere. Harmless (both do the identical, correct thing) and
 * out of scope for this contract to clean up, but noted here since it is the same shape of
 * redundancy {@link AncestorsContract} found and did remove (there, removal was already
 * needed to fix a real bug in the duplicated code; here there is no bug to justify touching it).
 *
 * <p>RESULT is waived: {@code Member.getDataMember()} is a bare interface method with no
 * default implementation in this repository (it lives in the Rolap engine) — the same
 * reasoning {@link CaptionContract} gives for {@code OlapElement.getCaption()}.
 */
public final class DataMemberContract {

    private DataMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("DataMember")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.DataMember")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(DataMemberFunDef.class, MEMBER)
            .resolvesWithCost(1, DataMemberFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, DataMemberFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(SET)          // Set does not convert to Member
            .rejects(LEVEL)         // Level does not convert to Member
            .rejects(NUMERIC)
            .rejects()              // arity 0
            .rejects(MEMBER, MEMBER)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("member reference",              "[Geo].[All Geo].[North].DataMember")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Geo].DataMember")

            .waive(Promise.RESULT,
                    "Member.getDataMember() is a bare interface method with no default "
                            + "implementation in this repository (see the class Javadoc) — its value for "
                            + "a member with no configured data member cannot be verified here.")

            .dependsOn("[Geo].[All Geo].[North].DataMember")

            // A scalar call has no set to shape. That it answers VALUE was asserted in
            // prose here and never checked, so it is a case now.
            .scalarResultStyle("[Geo].[All Geo].[North].DataMember.Name")
            .build();
}
