/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.as;

import org.eclipse.daanse.olap.testkit.function.FunctionContract;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.ITERABLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.testkit.function.FunctionContract.Promise;

/**
 * The contract of the MDX operator {@code AS} ({@code org.eclipse.daanse.olap.function.def.as},
 * {@code AsAliasFunDef} via {@code AsAliasResolver}) — {@code <Set Expression> AS <Alias>}.
 *
 * <p>{@code AsAliasResolver.resolve} is not a general-purpose resolver: by the time it runs,
 * the parser has already turned the alias identifier into a {@code NamedSetExpression} and
 * cast {@code args[1]} to it unconditionally, with no bounds check on {@code args.length}
 * either. That precondition always holds for real {@code "X as t"} MDX — the parser
 * guarantees it — but this test kit's generic stub-based probing (arity/type sweeps used by
 * promises 3, 4 and 6) cannot supply a real {@code NamedSetExpression} and trips it
 * immediately: an empty probe throws {@code ArrayIndexOutOfBoundsException} reading
 * {@code args[0]}, and any two-argument probe throws {@code ClassCastException} casting a
 * stub to {@code NamedSetExpression}. So those promises are waived here, not because the
 * behavior is untested, but because generic type-stub resolution genuinely cannot exercise
 * this atom without crashing — see {@code AsAliasFunDefTest} (mondrian test suite) for
 * real, parser-driven coverage of {@code AS} instead.
 */
public final class AsContract {

    private AsContract() {
    }

    private static final String RESOLVER_NEEDS_A_REAL_NAMED_SET_EXPRESSION =
            "AsAliasResolver.resolve casts args[1] to NamedSetExpression unconditionally (and reads "
                    + "args[0]/args[1] with no bounds check) — a precondition only the parser can satisfy. "
                    + "Generic stub-based probing throws ArrayIndexOutOfBoundsException or ClassCastException "
                    + "instead of returning Optional.empty(); see AsAliasFunDefTest for real MDX coverage.";

    public static final FunctionContract CONTRACT = FunctionContract.of("AS")
            .atom(InfixOperationAtom.class)
            .signatures("<Set> AS <String>")
            .returns(SET)
            .arity(2, 2)

            .waive(Promise.RESOLUTION, RESOLVER_NEEDS_A_REAL_NAMED_SET_EXPRESSION)
            .waive(Promise.SIGNATURE, RESOLVER_NEEDS_A_REAL_NAMED_SET_EXPRESSION
                    + " hasDeclaredSignature() itself would pass; it is waived only because "
                    + "declaredSignatureMatchesAcceptedCalls() shares this promise and cannot run.")
            .waive(Promise.EDGE, RESOLVER_NEEDS_A_REAL_NAMED_SET_EXPRESSION)

            // Grounded in AsAliasFunDefTest: aliasing a set is transparent, and aliasing a
            // single member implicitly wraps it as a singleton set (testAsWithAliasMemberImplicitSet).
            .valueKnownDefect("Count([Geo].Members as t)", "3",
                            "the name introduced by AS never comes into scope, so no expression"
                            + " using an alias can be validated")
            .valueKnownDefect("SetToStr([Geo].[All Geo].[North] as t)", "{[Geo].[Region].[F]}",
                            "the name introduced by AS never comes into scope, so no expression"
                            + " using an alias can be validated")

            // AsAliasCalc delegates straight to the aliased expression's own evaluation; the
            // alias itself introduces no dependency of its own.
            .dependsOn("[Geo].Members as t")

            // AsAliasCalc extends AbstractNestedTupleIteratorCalc, not the TupleList
            // base every other set-returning contract in this suite uses — it is iterator-shaped
            // by construction, so only ITERABLE is asserted here.
            .resultStyle("[Geo].Members as t", ITERABLE, ITERABLE)
            .build();
}
