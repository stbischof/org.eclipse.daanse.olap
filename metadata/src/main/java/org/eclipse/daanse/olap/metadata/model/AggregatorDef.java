/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.metadata.model;

/**
 * How a measure aggregates. The engine derives from it whether and how it may roll cells up
 * itself; an average never rolls up, a count rolls up as a sum.
 */
public sealed interface AggregatorDef permits AggregatorDef.Standard, AggregatorDef.Custom,
        AggregatorDef.OverOrderedValues {

    String name();

    /** The aggregators every engine knows. */
    enum Standard implements AggregatorDef {
        SUM, COUNT, MIN, MAX, AVG, DISTINCT_COUNT, NONE
    }

    /** An aggregator an engine looks up by name. */
    record Custom(String name) implements AggregatorDef {
    }

    /**
     * An aggregator that reads the values of a cell's facts in an order and makes one value of them: the cell is
     * computed by the source over its facts, never rolled up from other cells. The order is given by columns of the
     * fact rows; a source that holds its facts itself orders by the fact values named like the column's qualified
     * name ({@code ColumnDef.field("Seq")} is {@code Seq}, a column of a table {@code table.column}).
     */
    sealed interface OverOrderedValues extends AggregatorDef permits TextAgg, NthValue, BitAgg, Percentile {

        /** The columns the values are ordered by, first one first; empty for the order the facts come in. */
        java.util.List<org.eclipse.daanse.olap.metadata.relational.OrderedColumnDef> orderBy();

        /** The cell's value from the values of its facts, already in order; {@code null} values included. */
        Object aggregate(java.util.List<Object> valuesInOrder);
    }

    /**
     * The texts of the facts joined by a separator, as SQL's LISTAGG/STRING_AGG.
     *
     * @param distinct  whether a text that occurs again is left out
     * @param separator what stands between two texts; {@code null} for a comma
     * @param coalesce  what stands for a fact without a text; {@code null} to leave it out
     * @param onOverflowTruncate what the database appends when the text is cut, where it cuts; {@code null} for none
     */
    record TextAgg(boolean distinct, String separator, String coalesce, String onOverflowTruncate,
            java.util.List<org.eclipse.daanse.olap.metadata.relational.OrderedColumnDef> orderBy)
            implements OverOrderedValues {

        public TextAgg {
            orderBy = java.util.List.copyOf(orderBy);
        }

        @Override
        public String name() {
            return "textagg";
        }

        @Override
        public Object aggregate(java.util.List<Object> valuesInOrder) {
            // a distinct text over values in an order of other columns keeps every value, as the SQL databases do
            // where DISTINCT and an ORDER BY of another column do not combine
            java.util.Collection<String> texts = distinct && orderBy.isEmpty() ? new java.util.LinkedHashSet<>()
                    : new java.util.ArrayList<>();
            for (Object value : valuesInOrder) {
                Object text = value != null ? value : coalesce;
                if (text != null) {
                    texts.add(String.valueOf(text));
                }
            }
            return texts.isEmpty() ? null : String.join(separator == null ? "," : separator, texts);
        }
    }

    /**
     * The n-th value of the facts in order, counted from 1; empty when there are fewer (an aggregate over the
     * cell's facts, not a window over other cells).
     *
     * @param ignoreNulls whether facts without a value are skipped in the count
     */
    record NthValue(int n, boolean ignoreNulls,
            java.util.List<org.eclipse.daanse.olap.metadata.relational.OrderedColumnDef> orderBy)
            implements OverOrderedValues {

        public NthValue {
            n = Math.max(1, n);
            orderBy = java.util.List.copyOf(orderBy);
        }

        @Override
        public String name() {
            return "nthvalue";
        }

        @Override
        public Object aggregate(java.util.List<Object> valuesInOrder) {
            java.util.List<Object> candidates = ignoreNulls
                    ? valuesInOrder.stream().filter(java.util.Objects::nonNull).toList() : valuesInOrder;
            return candidates.size() < n ? null : candidates.get(n - 1);
        }
    }

    /**
     * The bits of the facts' whole numbers combined, as SQL's bit_and/bit_or/bit_xor, negated when asked.
     */
    record BitAgg(Operation operation, boolean not) implements OverOrderedValues {

        public enum Operation {
            AND, OR, XOR
        }

        @Override
        public String name() {
            return "bitagg";
        }

        @Override
        public java.util.List<org.eclipse.daanse.olap.metadata.relational.OrderedColumnDef> orderBy() {
            return java.util.List.of();
        }

        @Override
        public Object aggregate(java.util.List<Object> valuesInOrder) {
            Long result = null;
            for (Object value : valuesInOrder) {
                if (!(value instanceof Number number)) {
                    continue;
                }
                long bits = number.longValue();
                result = result == null ? bits : switch (operation) {
                case AND -> result & bits;
                case OR -> result | bits;
                case XOR -> result ^ bits;
                };
            }
            return result == null ? null : not ? ~result : result;
        }
    }

    /**
     * The value at a fraction of the facts' numbers in ascending order: discrete picks a value of the facts, in its
     * own type (quantile_disc); continuous interpolates between the two values around it (quantile_cont).
     *
     * @param fraction   between 0 and 1
     * @param continuous whether to interpolate
     * @param column     the column the numbers are read and ordered by, where the facts are read from tables
     */
    record Percentile(double fraction, boolean continuous,
            org.eclipse.daanse.olap.metadata.relational.OrderedColumnDef column) implements OverOrderedValues {

        @Override
        public String name() {
            return "percentile";
        }

        @Override
        public java.util.List<org.eclipse.daanse.olap.metadata.relational.OrderedColumnDef> orderBy() {
            return java.util.List.of();
        }

        @Override
        public Object aggregate(java.util.List<Object> valuesInOrder) {
            java.util.List<Number> numbers = valuesInOrder.stream().filter(v -> v instanceof Number).map(v -> (Number) v)
                    .sorted(java.util.Comparator.comparingDouble(Number::doubleValue)).toList();
            if (numbers.isEmpty()) {
                return null;
            }
            if (!continuous) {
                int index = (int) Math.ceil(fraction * numbers.size()) - 1;
                return numbers.get(Math.max(0, Math.min(numbers.size() - 1, index)));
            }
            double position = fraction * (numbers.size() - 1);
            int lower = (int) Math.floor(position);
            int upper = (int) Math.ceil(position);
            double low = numbers.get(lower).doubleValue();
            return low + (position - lower) * (numbers.get(upper).doubleValue() - low);
        }
    }
}
