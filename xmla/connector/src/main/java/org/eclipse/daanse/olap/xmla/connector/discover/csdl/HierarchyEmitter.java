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
package org.eclipse.daanse.olap.xmla.connector.discover.csdl;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.OrderByProperty;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.BiFactory;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.ContainsHiddenMembersType;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.SourceType;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TDefaultAggregateFunction;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TDocumentation;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TEntityType;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.THideMembers;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.THierarchy;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TLevel;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TProperty;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TPropertyRef;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TPropertyRefs;
import org.eclipse.daanse.xmla.model.csdl.v2.bi.TPropertyStatistics;
import org.eclipse.daanse.xmla.model.csdl.v2.edm.EdmFactory;
import org.eclipse.daanse.xmla.model.csdl.v2.edm.TEntityProperty;
import org.eclipse.daanse.olap.api.relational.RelationalLevel;

/**
 * The bi:Hierarchy elements of a dimension entity, level by level.
 * <p>
 * Not everything a hierarchy can be has a CSDL form: a parent-child hierarchy
 * has none, and its columns are emitted flat instead.
 */
public final class HierarchyEmitter {

    private final BiFactory bi = BiFactory.eINSTANCE;
    private final EdmFactory edm = EdmFactory.eINSTANCE;
    private final EmitContext ctx;
    private final TypeMapper typeMapper;
    private final DisplayFolderBuilder folders;

    public HierarchyEmitter(EmitContext ctx, TypeMapper typeMapper, DisplayFolderBuilder folders) {
        super();
        this.ctx = ctx;
        this.typeMapper = typeMapper;
        this.folders = folders;
    }

    public void emitDimension(Dimension dimension, org.eclipse.daanse.xmla.model.csdl.v2.edm.TEntityType edmEntity,
            TEntityType biEntity) {
        if (dimension.isMeasures()) {
            return;
        }
        if (dimension.getDimensionType() == DimensionType.TIME_DIMENSION) {
            biEntity.setContents("Time");
        }
        ctx.reader().getDimensionHierarchies(dimension).stream()
                .sorted(Comparator.comparingInt(Hierarchy::getOrdinalInCube))
                .forEach(h -> emitHierarchy(h, edmEntity, biEntity));
    }

    private void emitHierarchy(Hierarchy hierarchy, org.eclipse.daanse.xmla.model.csdl.v2.edm.TEntityType edmEntity,
            TEntityType biEntity) {
        List<? extends Level> levels = ctx.reader().getHierarchyLevels(hierarchy);
        List<? extends Level> real = levels.stream().filter(l -> !l.isAll()).filter(l -> l.getLevelType() != null)
                .toList();
        if (real.isEmpty()) {
            ctx.info("Hierarchie {} ohne emittierbare Level uebersprungen", hierarchy.getUniqueName());
            return;
        }

        THierarchy tHierarchy = bi.createTHierarchy();
        tHierarchy.setName(ctx.mangle(hierarchy.getUniqueName()));
        tHierarchy.setReferenceName(hierarchy.getName());
        if (!Objects.equals(hierarchy.getCaption(), hierarchy.getName())) {
            tHierarchy.setCaption(hierarchy.getCaption());
        }
        if (!hierarchy.isVisible()) {
            tHierarchy.setHidden(true);
        }
        docSummary(hierarchy.getDescription()).ifPresent(tHierarchy::setDocumentation);

        if (real.stream().anyMatch(Level::isParentChild)) {
            ctx.warn("parent-child hierarchy {} has no bi:Hierarchy form; its columns are " + "emitted flat",
                    hierarchy.getUniqueName());
            real.forEach(l ->  {
                if (l.isParentChild()) {
                    TEntityProperty levelProp = emitLevelProperty(l, edmEntity);
                    tHierarchy.getLevel().add(emitLevel(l, levelProp));
                }
            });
        } else {
            for (Level level : real) {
                TEntityProperty levelProp = emitLevelProperty(level, edmEntity);
                tHierarchy.getLevel().add(emitLevel(level, levelProp));
                emitMemberProperties(level, levelProp, edmEntity);
            }
        }
        biEntity.getHierarchy().add(tHierarchy);

        folders.forHierarchy(biEntity, hierarchy.getDisplayFolder(), tHierarchy.getName());
        if (hierarchy.hasAll() && hierarchy.getDefaultMember() != hierarchy.getAllMember()) {
            ctx.debug("the default member {} of {} cannot be expressed in CSDL",
                    hierarchy.getDefaultMember().getUniqueName(), hierarchy.getUniqueName());
        }
    }

    private TLevel emitLevel(Level level, TEntityProperty sourceProp) {
        TLevel tLevel = bi.createTLevel();
        tLevel.setName(ctx.mangle(level.getUniqueName()));
        if (!Objects.equals(level.getCaption(), level.getName())) {
            tLevel.setCaption(level.getCaption());
        } else {
            tLevel.setCaption(level.getName());
        }
        SourceType source = bi.createSourceType();
        org.eclipse.daanse.xmla.model.csdl.v2.bi.TPropertyRef ref = bi.createTPropertyRef();
        ref.setName(ctx.requireProperty(sourceProp.getName()));
        source.setPropertyRef(ref);
        tLevel.setSource(source);

        hideMembersOf(level).ifPresent(hm -> {
            ContainsHiddenMembersType chm = bi.createContainsHiddenMembersType();
            chm.setHideMembers(hm);
            tLevel.setContainsHiddenMembers(chm);
        });
        return tLevel;
    }

    private Optional<THideMembers> hideMembersOf(Level level) {
        return switch (level.getHideMemberCondition()) {
        case NEVER -> Optional.empty();
        case IF_BLANK_NAME -> Optional.of(THideMembers.NO_NAME);
        case IF_PARENTS_NAME -> Optional.of(THideMembers.PARENT_NAME);
        };
    }

    private TEntityProperty emitLevelProperty(Level level,
            org.eclipse.daanse.xmla.model.csdl.v2.edm.TEntityType edmEntity) {
        TEntityProperty p = edm.createTEntityProperty();
        p.setName(ctx.mangle(level.getUniqueName()));
        // The property carries the member names: a level with a name column is typed
        // by that column, not by its key (Customers.Name: numeric id, string name).
        Optional<Property> nameProperty = nameProperty(level);
        if (nameProperty.isPresent()) {
            typeMapper.apply(p, nameProperty.get().getType());
        } else {
            // A level that is not read from a column has names for keys.
            typeMapper.apply(p, level instanceof RelationalLevel relational ? relational.getDatatype()
                    : org.eclipse.daanse.sql.model.type.Datatype.VARCHAR);
        }
        TProperty biProp = bi.createTProperty();
        biProp.setHidden(true);
        biProp.setCaption(level.getName());
        biProp.setReferenceName(level.getUniqueName().replaceAll("\\[", "").replaceAll("\\]", ""));
        biProp.setDefaultAggregateFunction(TDefaultAggregateFunction.NONE);
        orderByOf(level).ifPresent(biProp::setOrderBy);
        statisticsOf(level).ifPresent(biProp::setStatistics);
        p.setBiProperty(biProp);
        edmEntity.getProperty().add(p);
        return p;
    }

    private static Optional<Property> nameProperty(Level level) {
        Property[] properties = level.getProperties();
        if (properties == null) {
            return Optional.empty();
        }
        for (Property property : properties) {
            if (property.isInternal() && StandardProperty.NAME.getName().equals(property.getName())) {
                return Optional.of(property);
            }
        }
        return Optional.empty();
    }

    private Optional<TPropertyRefs> orderByOf(Level level) {
        if (level.getOrderByProperty().isPresent()) {
            OrderByProperty obp = level.getOrderByProperty().get();
            TPropertyRefs propertyRefs = bi.createTPropertyRefs();
            TPropertyRef propertyRef = bi.createTPropertyRef();
            propertyRef.setName(ctx.mangle(level.getUniqueName() + "_" + obp.property().getName()));
            propertyRefs.getPropertyRef().add(propertyRef);
            return Optional.of(propertyRefs);
        }
        return Optional.empty();
    }

    private Optional<TPropertyStatistics> statisticsOf(Level level) {
        int approx = level.getApproxRowCount();
        if (approx < 0) {
            return Optional.empty();
        }
        TPropertyStatistics stats = bi.createTPropertyStatistics();
        stats.setDistinctValueCount(approx);
        return Optional.of(stats);
    }

    private void emitMemberProperties(Level level, TEntityProperty levelProp,
            org.eclipse.daanse.xmla.model.csdl.v2.edm.TEntityType edmEntity) {
        for (Property property : level.getProperties()) {
            if (property.isInternal() || property.getName().startsWith("$")) {
                continue;
            }
            TEntityProperty p = edm.createTEntityProperty();
            p.setName(ctx.mangle(level.getUniqueName() + "_" + property.getName()));
            typeMapper.apply(p, property.getType());
            TProperty biProp = bi.createTProperty();
            //TPropertyRefs relatedTo = bi.createTPropertyRefs();
            //org.eclipse.daanse.xmla.model.csdl.v2.bi.TPropertyRef ref = bi.createTPropertyRef();
            //ref.setName(levelProp.getName());
            //relatedTo.getPropertyRef().add(ref);
            //biProp.setRelatedTo(relatedTo);

            if (levelProp.getBiProperty().getRelatedTo() == null) {
                levelProp.getBiProperty().setRelatedTo(bi.createTPropertyRefs());
            }
            org.eclipse.daanse.xmla.model.csdl.v2.bi.TPropertyRef pref = bi.createTPropertyRef();
            pref.setName(p.getName()); 
            levelProp.getBiProperty().getRelatedTo().getPropertyRef().add(pref);
            
            //biProp.setHidden(true);
            biProp.setReferenceName(level.getUniqueName().replaceAll("\\[", "").replaceAll("\\]", "") + "." + property.getName());
            biProp.setDefaultAggregateFunction(TDefaultAggregateFunction.NONE);
            if (!Objects.equals(property.getCaption(), property.getName())) {
                biProp.setCaption(property.getCaption());
            } else {
                biProp.setCaption(property.getName());
            }
            p.setBiProperty(biProp);
            edmEntity.getProperty().add(p);
        }
    }

    private Optional<TDocumentation> docSummary(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        TDocumentation doc = bi.createTDocumentation();
        doc.setSummary(description);
        return Optional.of(doc);
    }
}