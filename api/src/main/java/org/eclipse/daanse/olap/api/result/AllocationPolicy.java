/*
 * Licensed to Julian Hyde under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Julian Hyde licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.api.result;



/**
 * Enumeration of the policies that can be used to modify the values of
 * child cells when their parent cell is modified in a writeback operation.
 *
 * @see Cell#setValue
 *
 * @author jhyde
 * @since Aug 22, 2006
 */
public enum AllocationPolicy {
    /**
     * Every atomic cell that contributes to the updated cell will be
     * assigned an equal value that is:
     *
     *
     * &lt;atomic cell value&gt; =
     * &lt;value&gt; / Count(atomic cells contained in &lt;tuple&gt;)
     *
     */
    EQUAL_ALLOCATION,

    /**
     * Every atomic cell that contributes to the updated cell will be
     * changed according to:
     *
     *
     * &lt;atomic cell value&gt; = &lt;atomic cell value&gt; +
     * (&lt;value&gt; - &lt;existing value&gt;)  /
     * Count(atomic cells contained in &lt;tuple&gt;)
     *
     */
    EQUAL_INCREMENT,

    /**
     * Every atomic cell that contributes to the updated cell will be
     * assigned an equal value that is:
     *
     *
     * &lt;atomic cell value&gt; =
     * &lt;value&gt; * &lt;weight value expression&gt;
     *
     *
     * Takes an optional argument, {@code weight_value_expression}.
     * If {@code weight_value_expression} is not provided, the following
     * expression is assigned to it by default:
     *
     *
     * &lt;weight value expression&gt; =
     * &lt;atomic cell value&gt; / &lt;existing value&gt;
     *
     *
     * The value of {@code weight value expression} should be expressed
     * as a value between 0 and 1. This value specifies the ratio of the
     * allocated value you want to assign to the atomic cells that are
     * affected by the allocation. It is the client application programmer's
     * responsibilffity to create expressions whose rollup aggregate values
     * will equal the allocated value of the expression.
     */
    WEIGHTED_ALLOCATION,

    /**
     * Every atomic cell that contributes to the updated cell will be
     * changed according to:
     *
     *
     * &lt;atomic cell value&gt; = &lt;atomic cell value&gt; +
     * (&lt;value&gt; - &lt;existing value&gt;)  *
     * &lt;weight value expression&gt;
     *
     *
     * Takes an optional argument, {@code weight_value_expression}.
     * If {@code weight_value_expression} is not provided, the following
     * expression is assigned to it by default:
     *
     *
     * &lt;weight value expression&gt; =
     * &lt;atomic cell value&gt; / &lt;existing value&gt;
     *
     *
     * The value of {@code weight value expression} should be expressed
     * as a value between 0 and 1. This value specifies the ratio of the
     * allocated value you want to assign to the atomic cells that are
     * affected by the allocation. It is the client application programmer's
     * responsibility to create expressions whose rollup aggregate values
     * will equal the allocated value of the expression.
     */
    WEIGHTED_INCREMENT,
}
