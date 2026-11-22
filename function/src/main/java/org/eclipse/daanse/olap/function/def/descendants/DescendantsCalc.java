/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.descendants;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.tuple.TupleList;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.evaluator.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.fun.sort.Sorter;

public class DescendantsCalc extends AbstractNestedTupleListCalc{

    private final Flag flag;

    public DescendantsCalc(Type type, MemberCalc memberCalc, LevelCalc levelCalc, final Flag flag) {
        super(type, memberCalc, levelCalc);
        this.flag = flag;
    }

    @Override
    public TupleList evaluateInternal(Evaluator evaluator ) {
        MemberCalc memberCalc = getChildCalc(0, MemberCalc.class);
      LevelCalc levelCalc = getChildCalc(1, LevelCalc.class);
      final Evaluator context =
        evaluator.isNonEmpty() ? evaluator : null;
      final Member member = memberCalc.evaluate( evaluator );
      List<Member> result = new ArrayList<>();
      final CatalogReader schemaReader =
        evaluator.getCatalogReader();
      final Level level =
        levelCalc != null
          ? levelCalc.evaluate( evaluator )
          : member.getLevel();
      descendantsByLevel(
        schemaReader, member, level, result,
        flag.before, flag.self,
        flag.after, flag.leaves, context );
      Sorter.hierarchizeMemberList( result, false );
      return new UnaryTupleList( result );
    }

    /**
     * Finds all descendants of a member which are before/at/after a level, and/or are leaves (have no descendants) and
     * adds them to a result list.
     *
     * @param schemaReader Member reader
     * @param ancestor     Member to find descendants of
     * @param level        Level relative to which to filter, must not be null
     * @param result       Result list
     * @param before       Whether to output members above level
     * @param self         Whether to output members at level
     * @param after        Whether to output members below level
     * @param leaves       Whether to output members which are leaves
     * @param context      Evaluation context; determines criteria by which the result set should be filtered
     */
    private void descendantsByLevel(
      CatalogReader schemaReader,
      Member ancestor,
      Level level,
      List<Member> result,
      boolean before,
      boolean self,
      boolean after,
      boolean leaves,
      Evaluator context ) {
      // We find the descendants of a member by making breadth-first passes
      // down the hierarchy. Initially the list just contains the ancestor.
      // Then we find its children. We add those children to the result if
      // they fulfill the before/self/after conditions relative to the level.
      //
      // We add a child to the "fertileMembers" list if some of its children
      // might be in the result. Again, this depends upon the
      // before/self/after conditions.
      //
      // Note that for some member readers -- notably the
      // RestrictedMemberReader, when it is reading a ragged hierarchy -- the
      // children of a member do not always belong to the same level. For
      // example, the children of USA include WA (a state) and Washington
      // (a city). This is why we repeat the before/self/after logic for
      // each member.
      final int levelDepth = level.getDepth();
      List<Member> members = List.of( ancestor );
      // Each pass, "fertileMembers" has the same contents as "members",
      // except that we omit members whose children we are not interested
      // in. We allocate it once, and clear it each pass, to save a little
      // memory allocation.
      if ( leaves ) {
        assert !before && !self && !after;
        do {
          List<Member> nextMembers = new ArrayList<>();
          for ( Member member : members ) {
            final int currentDepth = member.getLevel().getDepth();
            if(currentDepth == levelDepth) {
              result.add( member );
            }
            else  {
              List<Member> childMembers =
                      schemaReader.getMemberChildren( member, context );
              if ( childMembers.isEmpty() ) {
                // this member is a leaf -- add it
                if ( currentDepth <= levelDepth ) {
                  result.add( member );
                }
              } else {
                // this member is not a leaf -- add its children
                // to the list to be considered next iteration
                if ( currentDepth <= levelDepth ) {
                  nextMembers.addAll( childMembers );
                }
              }
            }
          }
          members = nextMembers;
        } while ( !members.isEmpty() );
      } else {
        List<Member> fertileMembers = new ArrayList<>();
        do {
          fertileMembers.clear();
          for ( Member member : members ) {
            final int currentDepth = member.getLevel().getDepth();
            if ( currentDepth == levelDepth ) {
              if ( self ) {
                result.add( member );
              }
              if ( after ) {
                // we are interested in member's children
                fertileMembers.add( member );
              }
            } else if ( currentDepth < levelDepth ) {
              if ( before ) {
                result.add( member );
              }
              fertileMembers.add( member );
            } else {
              if ( after ) {
                result.add( member );
                fertileMembers.add( member );
              }
            }
          }
          members =
            schemaReader.getMemberChildren( fertileMembers, context );
        } while ( !members.isEmpty() );
      }
    }

}
