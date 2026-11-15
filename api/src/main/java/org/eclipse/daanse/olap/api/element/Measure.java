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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.api.element;

import java.util.Optional;
import java.util.Set;

public interface Measure extends Member {

    /**
     * Which of the dimensions this tuple names the measure does not reach.
     * <p>
     * Asked of the measure and not of a cube, because what a measure reaches is a property of the measure:
     * a catalog that groups its measures some other way than by cube can answer it too. A measure that
     * reaches everything - the usual case - answers with nothing.
     */
    default Set<Dimension> dimensionsNotReached(Member[] tuple) {
        return Set.of();
    }

    /**
     * The member that counts the facts behind this measure, where the catalog keeps one. A cell of such a
     * measure is empty when nothing was counted, even if the measure itself works out to zero.
     */
    default Optional<Member> factCount() {
        return Optional.empty();
    }

    /**
     * The level of this measure's own dimensionality that answers for {@code level}, or empty when the
     * measure does not reach it at all. A measure whose dimensionality is the one asked about answers with
     * the level itself.
     */
    default Optional<Level> levelReached(Level level) {
        return Optional.of(level);
    }

    /**
     * Whether this measure adds its cells up, so that a value written to a cell above can be spread over
     * the cells beneath it. A measure that counts distinct things or takes a minimum cannot be spread, and
     * a client is told so rather than being allowed to write nonsense.
     */
    default boolean addsUp() {
        return false;
    }
}
