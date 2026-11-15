/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
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
package org.eclipse.daanse.olap.function.def.crossjoin;

import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.element.PropertyBase;

/**
 * Mock implementation of {@link Member} for testing.
 *
 * @author <a>Richard M. Emberson</a>
 */
public class TestMember implements Member {
  private final String identifer;

  public TestMember( String identifer ) {
    this.identifer = identifer;
  }

  @Override
public String toString() {
    return identifer;
  }

  @Override
public int compareTo( Object o ) {
    TestMember other = (TestMember) o;
    return this.identifer.compareTo( other.identifer );
  }

  @Override
public Member getParentMember() {
    throw new UnsupportedOperationException();
  }

  @Override
public Level getLevel() {
    throw new UnsupportedOperationException();
  }

  @Override
public Hierarchy getHierarchy() {
    throw new UnsupportedOperationException();
  }

  @Override
public String getParentUniqueName() {
    throw new UnsupportedOperationException();
  }

  @Override
public MemberType getMemberType() {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isParentChildLeaf() {
    return false;
  }

  @Override
public boolean isParentChildPhysicalMember() {
    return false;
  }

  @Override
public void setName( String name ) {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isAll() {
    return false;
  }

  @Override
public boolean isMeasure() {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isNull() {
    return true;
  }

  @Override
public boolean isChildOrEqualTo( Member member ) {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isCalculated() {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isEvaluated() {
    throw new UnsupportedOperationException();
  }

  @Override
public int getSolveOrder() {
    throw new UnsupportedOperationException();
  }

  @Override
public Expression getExpression() {
    throw new UnsupportedOperationException();
  }

  @Override
public List<Member> getAncestorMembers() {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isCalculatedInQuery() {
    throw new UnsupportedOperationException();
  }

  @Override
public Object getPropertyValue( String propertyName ) {
    throw new UnsupportedOperationException();
  }

  @Override
public Object getPropertyValue( String propertyName, boolean matchCase ) {
    throw new UnsupportedOperationException();
  }

  @Override
public String getPropertyFormattedValue( String propertyName ) {
    throw new UnsupportedOperationException();
  }

  @Override
public void setProperty( String name, Object value ) {
    throw new UnsupportedOperationException();
  }

  @Override
public PropertyBase[] getProperties() {
    throw new UnsupportedOperationException();
  }

  @Override
public int getOrdinal() {
    throw new UnsupportedOperationException();
  }

  @Override
public Comparable getOrderKey() {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isHidden() {
    throw new UnsupportedOperationException();
  }

  @Override
public int getDepth() {
    throw new UnsupportedOperationException();
  }

  @Override
public Member getDataMember() {
    throw new UnsupportedOperationException();
  }

  @Override public boolean isOnSameHierarchyChain( Member otherMember ) {
    throw new UnsupportedOperationException();
  }

  @Override
public String getUniqueName() {
    throw new UnsupportedOperationException();
  }

  @Override
public String getName() {
    throw new UnsupportedOperationException();
  }

  @Override
public String getDescription() {
    throw new UnsupportedOperationException();
  }

  @Override
public OlapElement lookupChild(
          CatalogReader schemaReader, Segment s, MatchType matchType ) {
    throw new UnsupportedOperationException();
  }

  @Override
public String getQualifiedName() {
    throw new UnsupportedOperationException();
  }

  @Override
public String getCaption() {
    throw new UnsupportedOperationException();
  }

  @Override
public String getLocalized( LocalizedProperty prop, Locale locale ) {
    throw new UnsupportedOperationException();
  }

  @Override
public boolean isVisible() {
    throw new UnsupportedOperationException();
  }

  @Override
public Dimension getDimension() {
    return new MockDimension();
  }


  @Override
  public MetaData getMetaData() {
      throw new UnsupportedOperationException();

  }

  private static class MockDimension implements Dimension {
    @Override
	public List<? extends Hierarchy> getHierarchies() {
      throw new UnsupportedOperationException();
    }

    @Override
	public boolean isMeasures() {
      throw new UnsupportedOperationException();
    }

    @Override
	public DimensionType getDimensionType() {
      throw new UnsupportedOperationException();
    }

    @Override
	public Catalog getCatalog() {
      throw new UnsupportedOperationException();
    }

    @Override
	public String getUniqueName() {
      throw new UnsupportedOperationException();
    }

    @Override
	public String getName() {
      throw new UnsupportedOperationException();
    }

    @Override
	public String getDescription() {
      throw new UnsupportedOperationException();
    }

    @Override
	public boolean isVisible() {
      throw new UnsupportedOperationException();
    }

    @Override
	public OlapElement lookupChild(
        CatalogReader schemaReader,
        Segment s, MatchType matchType ) {
      throw new UnsupportedOperationException();
    }

    @Override
	public String getQualifiedName() {
      throw new UnsupportedOperationException();
    }

    @Override
	public String getCaption() {
      throw new UnsupportedOperationException();
    }

    @Override
	public String getLocalized( LocalizedProperty prop, Locale locale ) {
      throw new UnsupportedOperationException();
    }

    @Override
	public Hierarchy getHierarchy() {
      throw new UnsupportedOperationException();
    }

    @Override
	public Dimension getDimension() {
      throw new UnsupportedOperationException();
    }

    @Override
	public MetaData getMetaData() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Cube getCube() {
        return null;
    }

  }

}
