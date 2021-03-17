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

import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.assetmanager.api.query.Field;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Operator;
import com.mysema.query.types.Ops;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.BooleanOperation;
import com.mysema.query.types.expr.ComparableExpressionBase;

/**
 * Generic implementation to query {@link org.opencastproject.assetmanager.impl.persistence.SnapshotDto} fields.
 *
 * @param <A>
 *         The business type of the field, e.g. {@link org.opencastproject.assetmanager.api.Version}
 * @param <B>
 *         The JPA internal type of the field, e.g. {@link Long}
 */
public abstract class AbstractSnapshotField<A, B extends Comparable> implements Field<A>, EntityPaths {
  private final ComparableExpressionBase<B> path;

  /**
   * Create a new snapshot field.
   *
   * @param path a path to a snapshot field
   */
  public AbstractSnapshotField(ComparableExpressionBase<B> path) {
    this.path = path;
  }

  /**
   * Extract database type <code>B</code> from business type <code>A</code>.
   */
  protected abstract B extract(A a);

  @Override public Predicate eq(final A right) {
    return mkComparison(Ops.EQ, right);
  }

  @Override public Predicate eq(PropertyField<A> right) {
    return mkComparison(Ops.EQ, right);
  }

  @Override public Predicate lt(A right) {
    return mkComparison(Ops.LT, right);
  }

  @Override public Predicate lt(PropertyField<A> right) {
    return mkComparison(Ops.LT, right);
  }

  @Override public Predicate le(A right) {
    return mkComparison(Ops.LOE, right);
  }

  @Override public Predicate le(PropertyField<A> right) {
    return mkComparison(Ops.LOE, right);
  }

  @Override public Predicate gt(A right) {
    return mkComparison(Ops.GT, right);
  }

  @Override public Predicate gt(PropertyField<A> right) {
    return mkComparison(Ops.GT, right);
  }

  @Override public Predicate ge(A right) {
    return mkComparison(Ops.GOE, right);
  }

  @Override public Predicate ge(PropertyField<A> right) {
    return mkComparison(Ops.GOE, right);
  }

  @Override public Predicate exists() {
    return mkPredicate(path.isNotNull());
  }

  @Override public Predicate notExists() {
    return mkPredicate(path.isNull());
  }

  @Override public Order desc() {
    return new AbstractOrder() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk().order($(path.desc()));
      }
    };
  }

  @Override public Order asc() {
    return new AbstractOrder() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk().order($(path.asc()));
      }
    };
  }

  protected static Predicate mkPredicate(final BooleanExpression where) {
    return new AbstractPredicate() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk().from($Q_SNAPSHOT).where(where);
      }

      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk().where(new Fn<EntityPath<?>, BooleanExpression>() {
          @Override public BooleanExpression apply(EntityPath<?> from) {
            if (from instanceof QSnapshotDto) {
              return where;
            } else if (from instanceof QPropertyDto) {
              return new JPASubQuery().from(Q_SNAPSHOT)
                  .where(Q_SNAPSHOT.mediaPackageId.eq(Q_PROPERTY.mediaPackageId).and(where))
                  .exists();
            } else {
              throw new RuntimeException("BUG");
            }
          }
        });
      }
    };
  }

  /**
   * Create a predicate for comparisons with a constant value.
   */
  private Predicate mkComparison(final Operator<? super Boolean> op, A right) {
    return mkPredicate(BooleanOperation.create(op, path, ConstantImpl.create(extract(right))));
  }

  /**
   * Create a predicate for comparisons with a property field.
   */
  private Predicate mkComparison(final Operator<? super Boolean> op, final PropertyField<A> right) {
    return mkPredicate(PropertyPredicates.mkWhereSelect(right.name(), new Fn<QPropertyDto, Opt<BooleanExpression>>() {
      @Override public Opt<BooleanExpression> apply(QPropertyDto qPropertyDto) {
        return Opt.some(BooleanOperation.create(op, path, RuntimeTypes.convert(right).getPath(qPropertyDto)));
      }
    }));
  }
}
