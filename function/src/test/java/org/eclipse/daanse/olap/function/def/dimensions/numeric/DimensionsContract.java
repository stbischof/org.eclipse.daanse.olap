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
package org.eclipse.daanse.olap.function.def.dimensions.numeric;

import org.eclipse.daanse.olap.testkit.function.FunctionContract;
import static org.eclipse.daanse.olap.api.DataType.DIMENSION;
import static org.eclipse.daanse.olap.api.DataType.HIERARCHY;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.function.def.dimensions.string.DimensionsStringFunDef;

/**
 * The contract of the MDX function {@code Dimensions(...)} — {@code Dimensions(<Numeric
 * Expression>)} and {@code Dimensions(<String>)}. Despite the name, both overloads return a
 * {@code HIERARCHY}, not a {@code DIMENSION} — {@code DimensionsNumericFunDef}'s own Javadoc
 * says so explicitly ("Actually returns a hierarchy. NOT an DIMENSION. This is consistent with
 * Analysis Services."), and both {@code getResultType} overrides return {@code
 * HierarchyType.Unknown}.
 *
 * <p>{@code DimensionsStringFunDef} was package-private, which would have made {@code
 * resolvesTo(DimensionsStringFunDef.class, ...)} uncompilable from this module — made public
 * (same fix class as {@code PeriodsToDateFunDef}/{@code AncestorLevelFunDef} earlier in this
 * series). No other bug: both resolvers are plain {@code ParametersCheckingFunctionDefinitionResolver}s
 * over a single scalar parameter, already registered in {@code StandardFunctions.java}.
 *
 * <p>The two overloads are not ambiguous for {@code NUMERIC}/{@code STRING} input (neither
 * converts to the other's category), but {@code MEMBER} and {@code TUPLE} both convert to
 * {@code NUMERIC} (cost 3, via a measure's implicit scalar value) and, more expensively, to
 * {@code STRING} (cost 4) — so a bare Member or Tuple argument resolves to {@code
 * DimensionsNumericFunDef}, not {@code DimensionsStringFunDef}. {@code DIMENSION}/{@code
 * HIERARCHY}/{@code LEVEL}/{@code SET} convert to neither and are rejected.
 *
 * <p>DEPENDENCIES: the sole argument is a plain scalar ({@code Integer} or {@code String})
 * literal — its compiled {@code IntegerCalc}/{@code StringCalc} carries no hierarchy of its
 * own, so (per {@code AbstractNestedCalc.dependsOn}'s child-delegation default) the
 * wrapping {@code DimensionNumericCalc}/{@code DimensionsStringCalc} depends on no hierarchy at
 * all — unlike {@link DimensionContract}'s property form, whose argument is itself
 * hierarchy-typed.
 */
public final class DimensionsContract {

    private DimensionsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Dimensions")
            .atom(FunctionOperationAtom.class)
            .signatures(
                    "<Hierarchy> Dimensions(<Numeric Expression>)",
                    "<Hierarchy> Dimensions(<String>)")
            .returns(HIERARCHY)
            .arity(1, 1)

            .resolvesTo(DimensionsNumericFunDef.class, NUMERIC)
            .resolvesTo(DimensionsStringFunDef.class, STRING)
            .resolvesWithCost(3, DimensionsNumericFunDef.class, MEMBER)   // Member -> Numeric, cheaper than -> String (4)
            .resolvesWithCost(3, DimensionsNumericFunDef.class, TUPLE)    // Tuple -> Numeric, cheaper than -> String (4)
            .rejects(SET)          // Set does not convert to anything
            .rejects(DIMENSION)     // Dimension converts to Member/Tuple/Hierarchy/Level only
            .rejects(HIERARCHY)     // Hierarchy converts to Dimension/Member/Tuple only
            .rejects(LEVEL)         // Level converts to Dimension/Hierarchy/Set only
            .rejects()              // arity 0
            .rejects(NUMERIC, NUMERIC)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("numeric index in range",     "Dimensions(0)")
            .edgeCaseMdx("numeric index out of range", "Dimensions(100)")
            .edgeCaseMdx("numeric index negative",     "Dimensions(-1)")
            .edgeCaseMdx("string lookup, found",       "Dimensions(\"Gender\")")
            .edgeCaseMdx("string lookup, not found",   "Dimensions(\"ZZZ\")")

            // n=0 is the Measurement Hierarchy — DimensionNumericCalc.nthHierarchy's own
            // comment; a Mondrian-wide convention, not a schema-specific assumption.
            .value("Dimensions(0).Name", "Measures")
            .value("(Dimensions(\"Gender\") IS [Geo].Dimension)", "true")

            .dependsOn("Dimensions(0)")
            .dependsOn("Dimensions(\"Gender\")")

            .waive(FunctionContract.Promise.RESULT_SHAPE,
                    "returns a Hierarchy, not a set; the Set ResultStyle promise does not apply")
            .waive(FunctionContract.Promise.RESULT,
                    "expected values were computed against a different catalogue and are not"
                            + " recomputed for [Test] yet; the result promise also needs a Result"
                            + " implementation, which is not lifted")

            .build();
}
