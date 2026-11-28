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
package org.eclipse.daanse.olap.metadata.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.daanse.olap.findings.Finding;
import org.eclipse.daanse.olap.findings.Place;
import org.eclipse.daanse.olap.metadata.DescriptionRules;
import org.eclipse.daanse.olap.metadata.model.CalculatedMemberDef;
import org.eclipse.daanse.olap.metadata.model.CubeUsageDef;
import org.eclipse.daanse.olap.metadata.model.DimensionDef;
import org.eclipse.daanse.olap.metadata.model.MeasureDef;
import org.eclipse.daanse.olap.metadata.model.MeasureUsageDef;
import org.eclipse.daanse.olap.metadata.model.PhysicalCubeDef;
import org.eclipse.daanse.olap.metadata.model.VirtualCubeDef;

public final class VirtualCubeBuilder extends AbstractCubeBuilder<VirtualCubeBuilder> {

    private record PendingCube(String cubeName, boolean ignoreUnrelatedDimensions) {
    }

    private record PendingMeasure(String name, String cubeName, String measureName) {
    }

    private final List<PendingCube> cubes = new ArrayList<>();
    private final List<PendingMeasure> measures = new ArrayList<>();

    private static boolean isAMeasure(CalculatedMemberDef member) {
        String hierarchy = member.hierarchy();
        return hierarchy == null || hierarchy.isBlank() || "[Measures]".equalsIgnoreCase(hierarchy.trim());
    }

    VirtualCubeBuilder(String name, List<Finding> problems, Place catalogPath) {
        super(name, problems, catalogPath);
    }

    /**
     * Draws from a cube of the catalog; its measures ignore a dimension of the virtual cube they do not vary by
     * (SSAS: IgnoreUnrelatedDimensions is true unless the measure group says false).
     */
    public VirtualCubeBuilder cube(String cubeName) {
        return cube(cubeName, true);
    }

    /**
     * @param ignoreUnrelatedDimensions whether the measures of that cube ignore a dimension they do
     *                                  not vary by, instead of being empty there
     */
    public VirtualCubeBuilder cube(String cubeName, boolean ignoreUnrelatedDimensions) {
        cubes.add(new PendingCube(cubeName, ignoreUnrelatedDimensions));
        return this;
    }

    /** Takes over a measure of a cube drawn from, under its own name. */
    public VirtualCubeBuilder measure(String cubeName, String measureName) {
        return measure(measureName, cubeName, measureName);
    }

    public VirtualCubeBuilder measure(String nameInVirtualCube, String cubeName, String measureName) {
        measures.add(new PendingMeasure(nameInVirtualCube, cubeName, measureName));
        return this;
    }

    VirtualCubeDef build(Function<String, DimensionDef> shared, Function<String, PhysicalCubeDef> physical) {
        List<CubeUsageDef> cubeUsages = new ArrayList<>();
        for (PendingCube pending : cubes) {
            PhysicalCubeDef cube = physical.apply(pending.cubeName());
            if (cube == null) {
                problems.add(BuildFindings.of(DescriptionRules.M077, path, "draws from cube " + pending.cubeName()
                        + ", which is not a physical cube of the catalog"));
            } else {
                cubeUsages.add(new CubeUsageDef(cube, pending.ignoreUnrelatedDimensions()));
            }
        }
        List<MeasureUsageDef> measureUsages = new ArrayList<>();
        List<PhysicalCubeDef> formulasTakenFrom = new ArrayList<>();
        // the formulas taken over stand before the cube's own, as Mondrian lists them
        List<CalculatedMemberDef> takenOver = new ArrayList<>();
        for (PendingMeasure pending : measures) {
            PhysicalCubeDef cube = physical.apply(pending.cubeName());
            MeasureDef measure = cube == null ? null
                    : cube.measures().stream().filter(m -> m.name().equalsIgnoreCase(pending.measureName())).findFirst()
                            .orElse(null);
            CalculatedMemberDef formula = cube == null || measure != null ? null
                    : cube.calculatedMembers().stream()
                            .filter(m -> m.name().equalsIgnoreCase(pending.measureName()) && isAMeasure(m)).findFirst()
                            .orElse(null);
            if (measure != null) {
                measureUsages.add(new MeasureUsageDef(pending.name(), cube, measure));
            } else if (formula != null) {
                // A formula of the cube is taken over as a formula of this one, under the name asked for.
                takenOver.add(new CalculatedMemberDef(pending.name(), formula.hierarchy(), formula.parent(),
                        formula.formula(), formula.formatString(), formula.solveOrder(), formula.properties(),
                        formula.displayFolder(), formula.info()));
                if (!formulasTakenFrom.contains(cube)) {
                    formulasTakenFrom.add(cube);
                }
            } else {
                problems.add(BuildFindings.of(DescriptionRules.O10, path.then("measure", pending.name()),
                        "takes over measure " + pending.measureName() + " of cube " + pending.cubeName()
                                + ", which does not exist"));
            }
        }
        calculatedMembers.addAll(0, takenOver);
        // The stored measures of a cube a formula was taken from come along out of sight: the formula is
        // worked out from them and must find them by name, while a client sees only what was asked for.
        // This is Mondrian's rule: formulas are resolved against every measure of the base cube, then the
        // visible list is set back to the imported ones.
        for (PhysicalCubeDef cube : formulasTakenFrom) {
            for (MeasureDef stored : cube.measures()) {
                boolean named = measureUsages.stream().anyMatch(u -> u.name().equals(stored.name()))
                        || calculatedMembers.stream().anyMatch(c -> c.name().equals(stored.name()));
                if (!named) {
                    measureUsages.add(new MeasureUsageDef(stored.name(), cube, stored, true));
                }
            }
        }
        return new VirtualCubeDef(name, cubeUsages, resolveUsages(shared), measureUsages, calculatedMembers,
                namedSets, kpis, defaultMeasure, info());
    }
}
