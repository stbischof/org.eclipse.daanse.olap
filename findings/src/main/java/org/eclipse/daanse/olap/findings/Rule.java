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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.findings;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A rule a check holds a catalog to: what a finding of it weighs, the consequences a policy may give it and the one it
 * takes where no policy speaks.
 *
 * @param id        the rule's id
 * @param severity  the weight of a finding of the rule; a finding under {@link Consequence#ABORT} is an error whatever
 *                  this says
 * @param allowed   the consequences a policy may give the rule, never empty
 * @param byDefault the consequence where no policy speaks, one of {@code allowed}
 * @param says      what the rule asks of a catalog, one sentence
 */
public record Rule(RuleId id, Severity severity, Set<Consequence> allowed, Consequence byDefault, String says) {

    public Rule {
        Objects.requireNonNull(id, "the id of a rule");
        Objects.requireNonNull(severity, "the severity of rule " + id);
        Objects.requireNonNull(allowed, "the allowed consequences of rule " + id);
        Objects.requireNonNull(byDefault, "the default consequence of rule " + id);
        Objects.requireNonNull(says, "what rule " + id + " says");
        if (allowed.isEmpty()) {
            throw new IllegalArgumentException("the rule " + id + " allows no consequence");
        }
        allowed = Collections.unmodifiableSet(EnumSet.copyOf(allowed));
        if (!allowed.contains(byDefault)) {
            throw new IllegalArgumentException("the rule " + id + " defaults to " + byDefault
                    + ", which it does not allow (" + allowed + ")");
        }
    }

    /** A rule that allows only its default consequence. */
    public static Rule of(String id, Severity severity, Consequence consequence, String says) {
        return new Rule(RuleId.of(id), severity, EnumSet.of(consequence), consequence, says);
    }

    /** Whether a policy may give the rule this consequence. */
    public boolean allows(Consequence consequence) {
        return allowed.contains(consequence);
    }
}
