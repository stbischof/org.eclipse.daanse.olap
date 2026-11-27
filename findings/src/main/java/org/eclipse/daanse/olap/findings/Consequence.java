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

/** What is done about a finding; a rule says which of these a policy may choose for it. */
public enum Consequence {
    /** The load or the translation fails, with every finding of this consequence. */
    ABORT,
    /** The element or the row the finding is about is left out; the work goes on. */
    DROP,
    /** A stand-in takes the place of what the finding is about; what stands in is the rule's to say. */
    SUBSTITUTE,
    /** Nothing changes; the finding is reported. */
    REPORT
}
