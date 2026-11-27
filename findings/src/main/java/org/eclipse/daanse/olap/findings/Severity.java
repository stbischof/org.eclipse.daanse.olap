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

/** How much a finding weighs. */
public enum Severity {
    /** Something is wrong; under the consequence {@link Consequence#ABORT} the work stops. */
    ERROR,
    /** Something works here but is worth knowing: a client or another server may read it otherwise. */
    WARNING,
    /** A remark: something was done as the source has it, said so that it can be found. */
    INFO
}
