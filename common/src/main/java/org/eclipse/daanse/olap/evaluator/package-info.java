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
 *   Stefan Bischof (bipolis.org) - initial
 */

/**
 * Hooks a provider uses to answer parts of a query natively instead of through the interpreter.
 *
 * <p>
 * What this package expects of an implementation, and what it brings itself:
 * <ul>
 * <li>Every member that enters the context stack is a {@link
 * org.eclipse.daanse.olap.evaluator.CalculableMember}: the stack asks a member to push itself and to
 * name its solve order, so a member that cannot do so has no place in it. A member that arrives
 * without it is refused by name, not by class cast.</li>
 * <li>The stand-in for a slicer that holds a set of tuples is made here, not by the implementation.
 * An implementation that builds its own is doing work twice.</li>
 * <li>What an implementation wants to remember about such a slicer is opaque to this package: it is
 * handed to {@code describeSlicer} and given back untouched. Nothing here looks inside it.</li>
 * </ul>
 */
@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version("0.0.1")
package org.eclipse.daanse.olap.evaluator;
