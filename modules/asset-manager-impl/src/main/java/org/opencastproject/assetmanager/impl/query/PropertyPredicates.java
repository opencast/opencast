/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.assetmanager.impl.query;

import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;
import org.opencastproject.assetmanager.impl.query.DeleteQueryContribution.Where;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.types.expr.BooleanExpression;

/**
 * A place to share common predicate constructor methods for properties.
 */
public final class PropertyPredicates implements EntityPaths {
  private static final QSnapshotDto Q_SNAPSHOT_ALIAS = new QSnapshotDto("s");
  private static final QPropertyDto Q_PROPERTY_ALIAS = new QPropertyDto("p");

  public static final Opt NONE = Opt.none();
  public static final Fn<QPropertyDto, Opt<BooleanExpression>> NO_VALUE =
      new Fn<QPropertyDto, Opt<BooleanExpression>>() {
        @Override public Opt<BooleanExpression> apply(QPropertyDto qPropertyDto) {
          return NONE;
        }
      };

  private PropertyPredicates() {
  }

  /**
   * Create a 'where' expression for queries that filter by some property based criteria.
   *
   * @param propertyName the full qualified name of the property
   * @param mkValueExpression a function to create a property value expression;
   *        use the passed property DTO to create the expression
   */
  public static BooleanExpression mkWhereSelect(
          PropertyName propertyName,
          Fn<QPropertyDto, Opt<BooleanExpression>> mkValueExpression) {
    return mkWhereSelect(Opt.some(propertyName.getNamespace()), Opt.some(propertyName.getName()), mkValueExpression);
  }

  /**
   * Create a 'where' expression for queries that filter by some property based criteria.
   *
   * @param namespace
   *         the property namespace to use
   * @param propertyName
   *         the name of the property
   * @param mkValueExpression
   *         a function to create a property value expression; use the passed property DTO to create the expression
   */
  public static BooleanExpression mkWhereSelect(
      Opt<String> namespace,
      Opt<String> propertyName,
      Fn<QPropertyDto, Opt<BooleanExpression>> mkValueExpression) {
    return new JPASubQuery().from(Q_PROPERTY_ALIAS)
        .where(Q_SNAPSHOT.mediaPackageId.eq(Q_PROPERTY_ALIAS.mediaPackageId)
            .and(namespace.isSome()
                ? Q_PROPERTY_ALIAS.namespace.eq(namespace.get())
                // Just passing null like in the other predicates yields an NPE for some reason I do not know.
                // The isNotNull predicate prevents this.
                : Q_PROPERTY_ALIAS.namespace.isNotNull())
            .and(propertyName.isSome()
                ? Q_PROPERTY_ALIAS.propertyName.eq(propertyName.get())
                : null)
            .and(mkValueExpression.apply(Q_PROPERTY_ALIAS).orNull()))
        .exists();
  }

  public static Where mkWhereDelete(
          final PropertyName propertyName,
          final Fn<QPropertyDto, Opt<BooleanExpression>> mkValueExpression) {
    return mkWhereDelete(Opt.some(propertyName.getNamespace()), Opt.some(propertyName.getName()), mkValueExpression);
  }

  public static Where mkWhereDelete(
          final Opt<String> namespace,
          final Opt<String> propertyName,
          final Fn<QPropertyDto, Opt<BooleanExpression>> mkValueExpression) {
    final Opt<BooleanExpression> valueExpression = mkValueExpression.apply(Q_PROPERTY);
    final BooleanExpression propertyPredicate = (namespace.isSome()
            ? Q_PROPERTY.namespace.eq(namespace.get())
            // Just passing null like in the other predicates yields an NPE for some reason I do not know.
            // The isNotNull predicate prevents this.
            : Q_PROPERTY.namespace.isNotNull())
            .and(propertyName.isSome() ? Q_PROPERTY.propertyName.eq(propertyName.get()) : null)
            .and(valueExpression.isSome() ? valueExpression.get() : null);
    //
    return new Where() {
      @Override public BooleanExpression fromSnapshot(QSnapshotDto e) {
        return new JPASubQuery()
                .from(Q_PROPERTY)
                .where(e.mediaPackageId.eq(Q_PROPERTY.mediaPackageId).and(propertyPredicate))
                .exists();
      }

      @Override public BooleanExpression fromProperty(QPropertyDto p) {
        return p.mediaPackageId.in(
                new JPASubQuery()
                        .from(p)
                        .where(propertyPredicate)
                        .distinct()
                        .list(p.mediaPackageId));
      }
    };
  }
}
