/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
* 
* ---- All changes after Fork in 2023 ------------------------
* 
* Project: Eclipse daanse
* 
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors after Fork in 2023:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/

package org.eclipse.daanse.olap.api.calc.profile;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.daanse.olap.api.execution.QueryTiming;

/**
 * Called when a statement has profile information.
 */
public interface ProfileHandler {
    /**
     * Called when a statement has finished executing.
     *
     * @param plan   Annotated plan
     * @param timing Query timings
     */
    public void explain(String plan, QueryTiming timing);

    /**
     * Called for each part of a statement that has finished executing, a named set, an axis, the slicer or the
     * query body, with the part's profile tree. The default renders the tree as text and hands it to
     * {@link #explain(String, QueryTiming)}; a handler that reads the numbers overrides this one.
     *
     * @param title   the part, such as {@code Axis (ROWS):}
     * @param profile the profile of the part's calc, or {@code null} for a part without one
     * @param timing  query timings
     */
    default void explain(String title, CalculationProfile profile, QueryTiming timing) {
        StringWriter text = new StringWriter();
        PrintWriter out = new PrintWriter(text);
        out.println(title);
        if (profile != null) {
            new SimpleCalculationProfileWriter(out).write(profile);
        }
        out.close();
        explain(text.toString(), timing);
    }
}
