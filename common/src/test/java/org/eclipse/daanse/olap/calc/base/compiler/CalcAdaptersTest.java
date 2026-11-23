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
 */
package org.eclipse.daanse.olap.calc.base.compiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleListCalc;
import org.eclipse.daanse.olap.api.exception.TypeConversionException;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantStringCalc;
import org.eclipse.daanse.olap.calc.base.type.booleanx.DoubleToBooleanCalc;
import org.eclipse.daanse.olap.calc.base.type.doublex.IntegerToDoubleCalc;
import org.eclipse.daanse.olap.calc.base.type.doublex.UnknownToDoubleCalc;
import org.eclipse.daanse.olap.calc.base.type.integer.DoubleToIntegerCalc;
import org.eclipse.daanse.olap.calc.base.type.member.UnknownToMemberCalc;
import org.eclipse.daanse.olap.calc.base.type.string.UnknownToStringCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.MemberCalcToTupleCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.CopyingListCalc;
import org.junit.jupiter.api.Test;

/**
 * Each conversion of the compiler alone, without a compiler, an evaluator or a validator: before, they were
 * reachable only through the whole compiler.
 */
class CalcAdaptersTest {

    private static final MemberType MEMBER = MemberType.Unknown;

    private static <T> T calc(Class<T> kind, org.eclipse.daanse.olap.api.type.Type type) {
        T calc = mock(kind);
        when(((Calc<?>) calc).getType()).thenReturn(type);
        return calc;
    }

    @Test
    void aMemberCalcStaysAndAnyOtherIsCastWhenEvaluated() {
        MemberCalc member = calc(MemberCalc.class, MEMBER);
        assertThat(CalcAdapters.asMember(MEMBER, member)).isSameAs(member);
        @SuppressWarnings("unchecked")
        Calc<Object> other = calc(Calc.class, MEMBER);
        assertThat(CalcAdapters.asMember(MEMBER, other)).isInstanceOf(UnknownToMemberCalc.class);
    }

    @Test
    void aDoubleFoldsAConstantWidensAnIntegerAndCastsTheRest() {
        DoubleCalc folded = CalcAdapters.asDouble(new ConstantIntegerCalc(CalcAdapters.INTEGER_TYPE, 3),
                NumericType.INSTANCE);
        assertThat(folded.evaluate(null)).isEqualTo(3.0);
        assertThat(CalcAdapters.asDouble(new ConstantIntegerCalc(CalcAdapters.INTEGER_TYPE, null),
                NumericType.INSTANCE).evaluate(null)).as("MDX NULL is Java null").isNull();
        assertThat(CalcAdapters.asDouble(calc(IntegerCalc.class, CalcAdapters.INTEGER_TYPE), NumericType.INSTANCE))
                .isInstanceOf(IntegerToDoubleCalc.class);
        DoubleCalc dbl = calc(DoubleCalc.class, NumericType.INSTANCE);
        assertThat(CalcAdapters.asDouble(dbl, NumericType.INSTANCE)).isSameAs(dbl);
        assertThat(CalcAdapters.asDouble(calc(StringCalc.class, StringType.INSTANCE), StringType.INSTANCE))
                .isInstanceOf(UnknownToDoubleCalc.class);
        assertThatThrownBy(() -> CalcAdapters.asDouble(new ConstantStringCalc(StringType.INSTANCE, "x"),
                StringType.INSTANCE)).isInstanceOf(TypeConversionException.class);
    }

    @Test
    void anIntegerTruncatesAConstantAndCutsADouble() {
        IntegerCalc folded = CalcAdapters.asInteger(new ConstantDoubleCalc(NumericType.INSTANCE, 3.7),
                NumericType.INSTANCE);
        assertThat(folded.evaluate(null)).isEqualTo(3);
        assertThat(CalcAdapters.asInteger(calc(DoubleCalc.class, NumericType.INSTANCE), NumericType.INSTANCE))
                .isInstanceOf(DoubleToIntegerCalc.class);
        IntegerCalc whole = new ConstantIntegerCalc(CalcAdapters.INTEGER_TYPE, 4);
        assertThat(CalcAdapters.asInteger(whole, CalcAdapters.INTEGER_TYPE)).as("a whole decimal stays")
                .isSameAs(whole);
        assertThat(CalcAdapters.asInteger(new ConstantStringCalc(NullType.INSTANCE, null), NullType.INSTANCE)
                .evaluate(null)).as("the NULL constant").isNull();
    }

    @Test
    void aTruthValueIsEveryNumberButNoughtAndRefusesText() {
        assertThat(CalcAdapters.asBoolean(new ConstantDoubleCalc(NumericType.INSTANCE, 0.5), NumericType.INSTANCE)
                .evaluate(null)).isTrue();
        assertThat(CalcAdapters.asBoolean(new ConstantDoubleCalc(NumericType.INSTANCE, -1.0), NumericType.INSTANCE)
                .evaluate(null)).isTrue();
        assertThat(CalcAdapters.asBoolean(new ConstantDoubleCalc(NumericType.INSTANCE, 0.0), NumericType.INSTANCE)
                .evaluate(null)).isFalse();
        assertThat(CalcAdapters.asBoolean(new ConstantDoubleCalc(NumericType.INSTANCE, null), NumericType.INSTANCE)
                .evaluate(null)).as("NULL is false").isFalse();
        assertThat(CalcAdapters.asBoolean(calc(DoubleCalc.class, NumericType.INSTANCE), NumericType.INSTANCE))
                .isInstanceOf(DoubleToBooleanCalc.class);
        BooleanCalc bool = calc(BooleanCalc.class, org.eclipse.daanse.olap.api.type.BooleanType.INSTANCE);
        assertThat(CalcAdapters.asBoolean(bool, bool.getType())).isSameAs(bool);
        assertThatThrownBy(() -> CalcAdapters.asBoolean(new ConstantStringCalc(StringType.INSTANCE, "x"),
                StringType.INSTANCE)).isInstanceOf(TypeConversionException.class);
    }

    @Test
    void aStringFoldsAConstantToItsTextAndCastsTheRest() {
        assertThat(CalcAdapters.asString(new ConstantDoubleCalc(NumericType.INSTANCE, 2.5)).evaluate(null))
                .isEqualTo("2.5");
        assertThat(CalcAdapters.asString(calc(DoubleCalc.class, NumericType.INSTANCE)))
                .isInstanceOf(UnknownToStringCalc.class);
    }

    @Test
    void aMemberInATuplePositionIsATupleOfOne() {
        TupleCalc tuple = CalcAdapters.asTuple(MEMBER, calc(MemberCalc.class, MEMBER));
        assertThat(tuple).isInstanceOf(MemberCalcToTupleCalc.class);
        assertThatThrownBy(() -> CalcAdapters.asTuple(NumericType.INSTANCE, calc(DoubleCalc.class, NumericType.INSTANCE)))
                .isInstanceOf(TypeConversionException.class);
    }

    @Test
    void aListIsCopiedOnlyForAConsumerThatChangesItAndOnlyWhenItIsNotMutableAlready() {
        TupleListCalc list = mock(TupleListCalc.class);
        when(list.getResultStyle()).thenReturn(ResultStyle.LIST);
        when(list.getType()).thenReturn(new org.eclipse.daanse.olap.api.type.SetType(MEMBER));
        assertThat(CalcAdapters.mutableIfAsked(list, false)).isSameAs(list);
        assertThat(CalcAdapters.mutableIfAsked(list, true)).isInstanceOf(CopyingListCalc.class);
        TupleListCalc mutable = mock(TupleListCalc.class);
        when(mutable.getResultStyle()).thenReturn(ResultStyle.MUTABLE_LIST);
        assertThat(CalcAdapters.mutableIfAsked(mutable, true)).isSameAs(mutable);
    }

    @Test
    void aTypeWithoutADimensionHasNoConstantDefaultHierarchy() {
        assertThat(CalcAdapters.constantDefaultHierarchy(MEMBER)).isNull();
    }
}
