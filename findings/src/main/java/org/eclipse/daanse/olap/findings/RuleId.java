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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The id of a rule: its family and its code within the family, written {@code family/code}, for example
 * {@code description/M012}. A code alone is not unique: two families may use the same one.
 *
 * @param family the family, lower-case letters and digits, starting with a letter
 * @param code   the code within the family, letters and digits
 */
public record RuleId(String family, String code) {

    private static final Pattern FAMILY = Pattern.compile("[a-z][a-z0-9]*");
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9]+");

    public RuleId {
        Objects.requireNonNull(family, "the family of a rule id");
        Objects.requireNonNull(code, "the code of a rule id");
        if (!FAMILY.matcher(family).matches()) {
            throw new IllegalArgumentException("the family '" + family + "' of a rule id is not lower-case letters"
                    + " and digits starting with a letter");
        }
        if (!CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("the code '" + code + "' of a rule id is not letters and digits");
        }
    }

    /** The id written as {@code family/code}. */
    public static RuleId of(String text) {
        Objects.requireNonNull(text, "the text of a rule id");
        int slash = text.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("the rule id '" + text + "' is not written family/code");
        }
        return new RuleId(text.substring(0, slash), text.substring(slash + 1));
    }

    /** The id as {@code family/code}. */
    @Override
    public String toString() {
        return family + "/" + code;
    }
}
