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
package org.eclipse.daanse.olap.result;

import org.eclipse.daanse.olap.cellformatter.FormatStringCellFormatter;
import org.eclipse.daanse.olap.api.result.CellValue;
import org.eclipse.daanse.olap.api.result.NotLoaded;
import org.eclipse.daanse.olap.api.result.NullValue;

/**
 * Everything a cell needs: value, format string, formatter and its ordinal
 * key. Placed in the result's cell container during evaluation and handed to
 * the cell as its constructor parameter.
 * <p>
 * Mutable during evaluation; not changed after evaluation has finished.
 */
public class CellInfo {
    /**
     * State of the cell: null while the cell has not been evaluated yet,
     * otherwise one of the {@link CellValue} states ({@link NullValue},
     * {@link org.eclipse.daanse.olap.api.result.ErrorValue},
     * {@link org.eclipse.daanse.olap.api.result.ObjectValue}).
     * {@link NotLoaded} is never stored; dirty-pass results are discarded.
     */
    public CellValue value;
    public String formatString;
    public FormatStringCellFormatter valueFormatter;
    public long key;

    /**
     * Creates a CellInfo representing the position of a cell.
     *
     * @param key Ordinal representing the position of a cell
     */
    public CellInfo(long key) {
        this(key, null, null, FormatStringCellFormatter.EMPTY);
    }

    /**
     * Creates a CellInfo with position, value, format string and formatter of a cell.
     *
     * @param key            Ordinal representing the position of a cell
     * @param value          Value of cell, or null if not yet known
     * @param formatString   Format string of cell, or null
     * @param valueFormatter Formatter for cell, or null
     */
    public CellInfo(long key, CellValue value, String formatString, FormatStringCellFormatter valueFormatter) {
        this.key = key;
        this.value = value;
        this.formatString = formatString;
        this.valueFormatter = valueFormatter;
    }

    @Override
    public int hashCode() {
        // Combine the upper 32 bits of the key with the lower 32 bits.
        // 'key ^ (key >>> 32)' was bad: CellKey.Two encodes (i, j) as
        // (i * Integer.MAX_VALUE + j), practically (i << 32, j), so for
        // k-bit i and j every hash code was k bits long too.
        return (int) (key ^ (key >>> 11) ^ (key >>> 24));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CellInfo that && that.key == this.key;
    }

    /**
     * Returns the formatted value of the cell.
     *
     * @return formatted value of the cell
     */
    public String getFormatValue() {
        // Unwrap the cell state for the formatter: NULL cells become Java
        // null (renders as the empty string unless the format string has a
        // NULL section), error cells the Throwable (produces "#ERR: ..."),
        // plain values unwrapped.
        final Object raw = switch (value) {
            case null -> null;
            case NullValue v -> null;
            default -> value.toLegacyValue();
        };
        return valueFormatter.format(raw, formatString);
    }
}
