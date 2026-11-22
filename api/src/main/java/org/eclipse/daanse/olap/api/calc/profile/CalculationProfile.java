/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.calc.profile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * The profile of one calc and, below it, of its children: the one model the text view, {@code EXPLAIN PLAN FOR} and
 * a handler read.
 */
public interface CalculationProfile {

    Class<?> clazz();

    /** The function the calc computes, as the compiler names it; empty where nobody named it. */
    Optional<String> name();

    Type type();

    ResultStyle resultStyle();

    /** The time of all evaluations together, in nanoseconds: the pauses between them do not count. */
    long nanos();

    /** The elements of the sets the calc returned, summed over its evaluations; 0 for a calc of one value. */
    long elements();

    /** The squares of those element counts, summed: with {@link #elements()} the spread of the set sizes. */
    long elementsSquared();

    Map<String, Object> additionalValues();

    List<CalculationProfile> childProfiles();

    /** The time of all evaluations together, {@link #nanos()}. */
    default Duration duration() {
        return Duration.ofNanos(nanos());
    }
}
