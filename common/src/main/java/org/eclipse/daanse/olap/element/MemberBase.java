/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2020 Hitachi Vantara and others
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

package org.eclipse.daanse.olap.element;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.query.Segment;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KeyMember;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MatchType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.formatter.MemberFormatter;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.fun.FunUtil;

/**
 * MemberBase is a partial implementation of {@link Member}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class MemberBase
  extends OlapElementBase
  implements Member {

  protected Member parentMember;
  protected Level level;
  protected String uniqueName;

  /**
   * Combines member type and other properties, such as whether the member is the 'all' or 'null' member of its
   * hierarchy and whether it is a measure or is calculated, into an integer field.
   *
   * The fields are:
   * bits 0, 1, 2 ({@link #FLAG_TYPE_MASK}) are member type;
   * bit 3 ({@link #FLAG_HIDDEN}) is set if the member is hidden;
   * bit 4 ({@link #FLAG_ALL}) is set if this is the all member of its
   * hierarchy;
   * bit 5 ({@link #FLAG_NULL}) is set if this is the null member of its
   * hierarchy;
   * bit 6 ({@link #FLAG_CALCULATED}) is set if this is a calculated
   * member.
   * bit 7 ({@link #FLAG_MEASURE}) is set if this is a measure.
   *
   *
   * NOTE: jhyde, 2007/8/10. It is necessary to cache whether the member is 'all', 'calculated' or 'null' in the
   * member's state, because these properties are used so often. If we used a virtual method call - say we made each
   * subclass implement 'boolean isNull()' - it would be slower. We use one flags field rather than 4 boolean fields to
   * save space.
   */
  protected final int flags;

  private static final int FLAG_TYPE_MASK = 0x07;
  private static final int FLAG_HIDDEN = 0x08;
  private static final int FLAG_ALL = 0x10;
  private static final int FLAG_NULL = 0x20;
  private static final int FLAG_CALCULATED = 0x40;
  private static final int FLAG_MEASURE = 0x80;

  /**
   * Cached values of {@link org.eclipse.daanse.olap.api.element.Member.MemberType} enumeration. Without caching, get excessive calls to
   * {@link Object#clone}.
   */
  private static final MemberType[] MEMBER_TYPE_VALUES = MemberType.values();
    private final static String mdxMemberName = "member ''{0}''";

    protected MemberBase(
    Member parentMember,
    Level level,
    MemberType memberType ) {
    this.parentMember = parentMember;
    this.level = level;
    this.flags = memberType.ordinal()
      | ( memberType == MemberType.ALL ? FLAG_ALL : 0 )
      | ( memberType == MemberType.NULL ? FLAG_NULL : 0 )
      | ( computeCalculated( memberType ) ? FLAG_CALCULATED : 0 )
      | ( level.getHierarchy().getDimension().isMeasures()
      ? FLAG_MEASURE
      : 0 );
  }

  protected MemberBase() {
    this.flags = 0;
    this.level = null;
  }

  @Override
public String getQualifiedName() {
    return MessageFormat.format( mdxMemberName, getUniqueName() );
  }

  @Override
public abstract String getName();

  @Override
public String getUniqueName() {
    return uniqueName;
  }

  @Override
public String getCaption() {
    // if there is a member formatter for the members level,
    //  we will call this interface to provide the display string
    MemberFormatter mf = getLevel().getMemberFormatter();
    if ( mf != null ) {
      return mf.format( this );
    }

    // fallback if formatter is null
    final String caption = super.getCaption();
    return ( caption != null )
      ? caption
      : getName();
  }

  @Override
public String getParentUniqueName() {
    return parentMember == null
      ? null
      : parentMember.getUniqueName();
  }

  @Override
public Dimension getDimension() {
    return level.getDimension();
  }

  @Override
public Hierarchy getHierarchy() {
    return level.getHierarchy();
  }

  @Override
public Level getLevel() {
    return level;
  }

  @Override
public MemberType getMemberType() {
    return MEMBER_TYPE_VALUES[ flags & FLAG_TYPE_MASK ];
  }

  @Override
public String getDescription() {
    return (String) getPropertyValue( StandardProperty.DESCRIPTION_PROPERTY.getName() );
  }

  @Override
public boolean isMeasure() {
    return ( flags & FLAG_MEASURE ) != 0;
  }

  @Override
public boolean isAll() {
    return ( flags & FLAG_ALL ) != 0;
  }

  @Override
public boolean isNull() {
    return ( flags & FLAG_NULL ) != 0;
  }

  @Override
public boolean isCalculated() {
    return ( flags & FLAG_CALCULATED ) != 0;
  }

  @Override
public boolean isEvaluated() {
    // should just call isCalculated(), but called in tight loops
    // and too many subclass implementations for jit to inline properly?
    return ( flags & FLAG_CALCULATED ) != 0;
  }

  @Override
public OlapElement lookupChild(
    CatalogReader schemaReader,
    Segment childName,
    MatchType matchType ) {
    return schemaReader.lookupMemberChildByName(
      this, childName, matchType );
  }

  // implement Member
  @Override
public Member getParentMember() {
    return parentMember;
  }

  // implement Member
  @Override
public boolean isChildOrEqualTo( Member member ) {
    // REVIEW: Using uniqueName to calculate ancestry seems inefficient,
    //   because we can't afford to store every member's unique name, so
    //   we want to compute it on the fly
    //assert !Bug.BugSegregateRolapCubeMemberFixed;
    return ( member != null ) && isChildOrEqualTo( member.getUniqueName() );
  }

  /**
   * Returns whether this Member's unique name is equal to, a child of, or a descendent of a member whose
   * unique name is
   * uniqueName.
   */
  public boolean isChildOrEqualTo( String uniqueName ) {
    if ( uniqueName == null ) {
      return false;
    }

    return isChildOrEqualTo( this, uniqueName );
  }

  private static boolean isChildOrEqualTo( Member member, String uniqueName ) {
    while ( true ) {
      String thisUniqueName = member.getUniqueName();
      if ( thisUniqueName.equals( uniqueName ) ) {
        // found a match
        return true;
      }
      // try candidate's parentMember
      member = member.getParentMember();
      if ( member == null ) {
        // have reached root
        return false;
      }
    }
  }

  /**
   * Computes the value to be returned by {@link #isCalculated()}, so it can be cached in a variable.
   *
   * @param memberType Member type
   * @return Whether this member is calculated
   */
  protected boolean computeCalculated( final MemberType memberType ) {
    // If the member is not created from the "with member ..." MDX, the
    // calculated will be null. But it may be still a calculated measure
    // stored in the cube.
    return isCalculatedInQuery() || memberType == MemberType.FORMULA;
  }

  @Override
public int getSolveOrder() {
    return -1;
  }

  /**
   * Returns the expression by which this member is calculated. The expression is not null if and only if the
   * member is
   * not calculated.
   *
   *  (return ! = null) == (isCalculated())
   */
  @Override
public Expression getExpression() {
    return null;
  }

  // implement Member
  @Override
public List<Member> getAncestorMembers() {

      // if i see this member i am allowed to see the ancestors
    final CatalogReader schemaReader =
      getDimension().getCatalog().getCatalogReaderWithDefaultRole();
    final ArrayList<Member> ancestors = new ArrayList<>();
    schemaReader.getMemberAncestors( this, ancestors );
    return ancestors;
  }

  /**
   * Returns the ordinal of this member within its hierarchy. The default implementation returns -1.
   */
  @Override
public int getOrdinal() {
    return -1;
  }

  /**
   * Returns the order key of this member among its siblings. The default implementation returns null.
   */
  @Override
public Comparable getOrderKey() {
    return null;
  }

  /**
   * How far down this member sits. Answered from its level, which is where a member sits unless a
   * hierarchy nests its members within one level; such a hierarchy answers from the tree instead.
   */
  @Override
public int getDepth() {
    return getLevel().getDepth();
  }

  /** The properties a member of this level carries, its own and those it inherits. */
  @Override
public Property[] getProperties() {
    return getLevel().getInheritedProperties();
  }

  /** Nothing, unless a member is told something about itself beyond its properties. */
  @Override
public MetaData getMetaData() {
    return OlapMetaDataBase.empty();
  }

  @Override
public boolean isHidden() {
    return false;
  }

  @Override
public Member getDataMember() {
    return null;
  }

  @Override
public String getPropertyFormattedValue( String propertyName ) {
    return getPropertyValue( propertyName ).toString();
  }

  @Override
public Object getPropertyValue( String propertyName ) {
    return getPropertyValue( propertyName, true );
  }

  /**
   * Answers the standard member properties that follow from the member's place
   * in its hierarchy. A subclass with its own property store overrides this and
   * falls back to {@code super} for the standard ones. Unknown or provider-specific
   * properties answer {@code null}.
   */
  @Override
public Object getPropertyValue( String propertyName, boolean matchCase ) {
    StandardProperty property = StandardProperty.lookup( propertyName, matchCase );
    if ( property == null ) {
      return null;
    }
    if ( property == StandardProperty.CAPTION || property == StandardProperty.MEMBER_CAPTION ) {
      return getCaption();
    } else if ( property == StandardProperty.SCHEMA_NAME ) {
      return getHierarchy().getDimension().getCatalog().getName();
    } else if ( property == StandardProperty.DIMENSION_UNIQUE_NAME ) {
      return getHierarchy().getDimension().getUniqueName();
    } else if ( property == StandardProperty.HIERARCHY_UNIQUE_NAME ) {
      return getHierarchy().getUniqueName();
    } else if ( property == StandardProperty.LEVEL_UNIQUE_NAME ) {
      return getLevel().getUniqueName();
    } else if ( property == StandardProperty.LEVEL_NUMBER ) {
      return getLevel().getDepth();
    } else if ( property == StandardProperty.MEMBER_UNIQUE_NAME ) {
      return getUniqueName();
    } else if ( property == StandardProperty.MEMBER_NAME ) {
      return getName();
    } else if ( property == StandardProperty.MEMBER_TYPE ) {
      return getMemberType().ordinal();
    } else if ( property == StandardProperty.MEMBER_ORDINAL ) {
      return getOrdinal();
    } else if ( property == StandardProperty.PARENT_LEVEL ) {
      Member parent = getParentMember();
      return parent == null ? 0 : parent.getLevel().getDepth();
    } else if ( property == StandardProperty.PARENT_UNIQUE_NAME ) {
      Member parent = getParentMember();
      return parent == null ? null : parent.getUniqueName();
    } else if ( property == StandardProperty.PARENT_COUNT ) {
      return getParentMember() == null ? 0 : 1;
    } else if ( property == StandardProperty.VISIBLE ) {
      return isVisible();
    } else if ( property == StandardProperty.KEY || property == StandardProperty.MEMBER_KEY ) {
      return keyValue();
    } else if ( property == StandardProperty.CHILDREN_CARDINALITY ) {
      return childCount();
    } else if ( property == StandardProperty.DESCRIPTION_PROPERTY ) {
      return null;
    }
    return null;
  }

  /**
   * The key that addresses this member where its facts are kept. A member that is addressed that way
   * says so by being a {@link KeyMember}; the all member stands for everything and reads as zero.
   */
  private Object keyValue() {
    if ( isAll() ) {
      return 0;
    }
    return this instanceof KeyMember keyed ? keyed.getKey() : null;
  }

  /**
   * How many children this member has, for the client that asks the member rather than reading them. An
   * art that would have to fetch them to count answers {@code null} unless counting is cheap for it.
   *
   * @return the number of children, or {@code null} when this art does not say
   */
  protected Integer childCount() {
    return null;
  }

  /**
   * Orders members of one hierarchy by their order key when both have one, else
   * by unique name. Members of different hierarchies do not compare; a
   * provider with a richer ordering overrides this.
   */
  @Override
public int compareTo( Object o ) {
    Member other = (Member) o;
    Comparable thisKey = getOrderKey();
    Comparable otherKey = other.getOrderKey();
    if ( thisKey != null && otherKey != null && thisKey.getClass() == otherKey.getClass() ) {
      @SuppressWarnings( "unchecked" )
      int c = thisKey.compareTo( otherKey );
      if ( c != 0 ) {
        return c;
      }
    }
    return getUniqueName().compareTo( other.getUniqueName() );
  }

  @Override
public boolean isParentChildPhysicalMember() {
    return false;
  }

  @Override
public boolean isParentChildLeaf() {
    return false;
  }

  @Override
public boolean isOnSameHierarchyChain( Member otherMember ) {
    return ( (MemberBase) otherMember ).isOnSameHierarchyChainInternal( this );
  }

  public boolean isOnSameHierarchyChainInternal( MemberBase otherMember ) {
    return FunUtil.isAncestorOf( otherMember, this, false ) || FunUtil.isAncestorOf( this, otherMember, false );
  }

  public abstract Object getCaptionValue();
}
