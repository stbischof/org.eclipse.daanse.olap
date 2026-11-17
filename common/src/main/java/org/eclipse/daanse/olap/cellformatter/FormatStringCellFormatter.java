/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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
package org.eclipse.daanse.olap.cellformatter;

import java.util.Locale;
import java.util.Map;

import org.eclipse.daanse.olap.api.formatter.CellFormatter;

/**
 * Formats a cell value according to a format string.
 * <p>
 * Every cell has a value, a format string (or a {@link CellFormatter}) and a
 * formatted value. There are many possible values, hence many formatted
 * strings, but only few format strings and formatters, so the formatters are
 * shared: one {@link LocaleCellFormatter} per {@link Locale} through
 * {@link #forLocale(Locale)}, and one {@link UserDefinedCellFormatter} per
 * user-defined formatter.
 */
public interface FormatStringCellFormatter {

    /**
     * Formats a value according to a format string.
     *
     * @param value        Value
     * @param formatString Format string
     * @return Formatted value
     */
    String format(Object value, String formatString);

    /** Formatter that always returns the empty string. */
    FormatStringCellFormatter EMPTY = (value, formatString) -> "";

    /** JVM-global map from Locale to formatter; holds only the few Locales in use. */
    Map<Locale, FormatStringCellFormatter> LOCALE_FORMATTERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The shared {@link LocaleCellFormatter} for a locale. */
    static FormatStringCellFormatter forLocale(Locale locale) {
        return LOCALE_FORMATTERS.computeIfAbsent(locale, LocaleCellFormatter::new);
    }
}
