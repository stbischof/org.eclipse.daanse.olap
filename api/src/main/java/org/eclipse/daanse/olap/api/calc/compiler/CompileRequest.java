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
 */
package org.eclipse.daanse.olap.api.calc.compiler;

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.olap.api.calc.ResultStyle;

/**
 * What a compile asks for: the kind of calc (a double, a member, a mutable list, ...) or, for {@link Kind#ANY}, the
 * plain calc of the expression -- under a result type and preferred {@link ResultStyle}s when {@link #as} gives them.
 * {@link ExpressionCompiler#compile(org.eclipse.daanse.olap.api.query.component.Expression, CompileRequest)} is the one
 * way in; the typed entries of the compiler are this method with a request of their kind.
 *
 * @param kind       the kind of calc asked for
 * @param resultType for {@link Kind#ANY}: the type to convert to, or null for the expression's own
 * @param styles     for {@link Kind#ANY}: the preferred result styles, or null for those of the compile around it
 */
public record CompileRequest(Kind kind, org.eclipse.daanse.olap.api.type.Type resultType, List<ResultStyle> styles) {

    /** The kinds of calc a compile can ask for; each is one typed entry of {@link ExpressionCompiler}. */
    public enum Kind {
        /** The calc of the expression as it is ({@code compile}; with a type and styles {@link CompileRequest#as}). */
        ANY,
        MEMBER, LEVEL, DIMENSION, HIERARCHY, INTEGER, STRING, DATE_TIME, BOOLEAN, DOUBLE, TUPLE,
        /** An immutable list ({@code compileList}). */
        LIST,
        /** A list the caller may change ({@code compileMutableList}). */
        MUTABLE_LIST,
        /** An iterable ({@code compileIterable}). */
        ITERABLE,
        /** A scalar of the generic kind ({@code compileScalar}). */
        SCALAR,
        /** A scalar of the exact type of the expression ({@code compileExactScalar}). */
        EXACT_SCALAR
    }

    public static final CompileRequest ANY = new CompileRequest(Kind.ANY, null, null);
    public static final CompileRequest MEMBER = of(Kind.MEMBER);
    public static final CompileRequest LEVEL = of(Kind.LEVEL);
    public static final CompileRequest DIMENSION = of(Kind.DIMENSION);
    public static final CompileRequest HIERARCHY = of(Kind.HIERARCHY);
    public static final CompileRequest INTEGER = of(Kind.INTEGER);
    public static final CompileRequest STRING = of(Kind.STRING);
    public static final CompileRequest DATE_TIME = of(Kind.DATE_TIME);
    public static final CompileRequest BOOLEAN = of(Kind.BOOLEAN);
    public static final CompileRequest DOUBLE = of(Kind.DOUBLE);
    public static final CompileRequest TUPLE = of(Kind.TUPLE);
    public static final CompileRequest LIST = of(Kind.LIST);
    public static final CompileRequest MUTABLE_LIST = of(Kind.MUTABLE_LIST);
    public static final CompileRequest ITERABLE = of(Kind.ITERABLE);
    public static final CompileRequest SCALAR = of(Kind.SCALAR);
    public static final CompileRequest EXACT_SCALAR = of(Kind.EXACT_SCALAR);

    public CompileRequest {
        Objects.requireNonNull(kind, "kind");
        if (kind != Kind.ANY && (resultType != null || styles != null)) {
            throw new IllegalArgumentException("only a request of kind ANY carries a result type and styles: " + kind);
        }
    }

    private static CompileRequest of(Kind kind) {
        return new CompileRequest(kind, null, null);
    }

    /**
     * The calc of the expression under a result type (null for its own) and the preferred styles, never null; once
     * the entry {@code compileAs}.
     */
    public static CompileRequest as(org.eclipse.daanse.olap.api.type.Type resultType, List<ResultStyle> styles) {
        Objects.requireNonNull(styles, "preferredResultTypes must not be null");
        return new CompileRequest(Kind.ANY, resultType, styles);
    }

    /** Whether the caller may change the list it gets. */
    public boolean mutable() {
        return kind == Kind.MUTABLE_LIST;
    }
}
