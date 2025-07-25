/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api;

import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * Parameter to a Query.
 *
 * A parameter is not an expression; see  ParameterExpr.
 *
 * @author jhyde
 * @since Jul 22, 2006
 */
public interface Parameter {
    /**
     * Returns the scope where this parameter is defined.
     *
     * @return Scope of the parameter
     */
    Scope getScope();

    /**
     * Returns the type of this Parameter.
     *
     * @return Type of the parameter
     */
    Type getType();

    /**
     * Returns the expression which provides the default value for this
     * Parameter. Never null.
     *
     * @return Default value expression of the parameter
     */
    Expression getDefaultExp();

    /**
     * Returns the name of this Parameter.
     *
     * @return Name of the parameter
     */
    String getName();

    /**
     * Returns the description of this Parameter.
     *
     * @return Description of the parameter
     */
    String getDescription();

    /**
     * Returns whether the value of this Parameter can be modified in a query.
     *
     * @return Whether parameter is modifiable
     */
    boolean isModifiable();

    /**
     * Returns the value of this parameter.
     *
     * If  #setValue(Object) has not been called, returns the default
     * value of this parameter.
     *
     * The type of the value is (depending on the type of the parameter)
     * a  String,  Number, or  Member.
     *
     * @return The value of this parameter
     */
    Object getValue();

    /**
     * Sets the value of this parameter.
     *
     * @param value Value of the parameter; must be a  String,
     *   a  Double, or a  org.eclipse.daanse.olap.api.element.Member
     */
    void setValue(Object value);

    /**
     * Returns whether the value of this parameter has been set.
     *
     * If the value has not been set, this parameter will return its default
     * value.
     *
     * Setting a parameter to {@code null} is not equivalent to unsetting it.
     * To unset a parameter, call  #unsetValue.
     *
     * @return Whether this parameter has been assigned a value
     */
    boolean isSet();

    /**
     * Unsets the value of this parameter.
     *
     * After calling this method, the parameter will revert to its default
     * value, as if  #setValue(Object) had not been called, and
     *  #isSet() will return {@code false}.
     */
    void unsetValue();

    /**
     * Scope where a parameter is defined.
     */
    enum Scope {
        System,
        Schema,
        Connection,
        Statement
    }
}
