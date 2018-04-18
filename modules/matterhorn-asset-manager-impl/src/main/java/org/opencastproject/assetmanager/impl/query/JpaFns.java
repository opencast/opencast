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

import org.opencastproject.util.data.Collections;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Fns;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.expr.BooleanExpression;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class JpaFns {
  private JpaFns() {
  }

  static final Fn2<BooleanExpression, BooleanExpression, BooleanExpression> and =
          new Fn2<BooleanExpression, BooleanExpression, BooleanExpression>() {
            @Override public BooleanExpression apply(BooleanExpression left, BooleanExpression right) {
              return left.and(right);
            }
          };
  static final Fn2<BooleanExpression, BooleanExpression, BooleanExpression> or =
          new Fn2<BooleanExpression, BooleanExpression, BooleanExpression>() {
            @Override public BooleanExpression apply(BooleanExpression left, BooleanExpression right) {
              return left.or(right);
            }
          };
  static final Fn<BooleanExpression, BooleanExpression> not =
          new Fn<BooleanExpression, BooleanExpression>() {
            @Override public BooleanExpression apply(BooleanExpression expr) {
              return expr.not();
            }
          };

  /**
   * Apply a boolean operation to two expressions. If one of the expressions is none the other one is returned.
   */
  static Opt<BooleanExpression> op(Fn2<BooleanExpression, BooleanExpression, BooleanExpression> f,
                                   Opt<BooleanExpression> a, Opt<BooleanExpression> b) {
    if (a.isNone()) {
      return b;
    } else if (b.isNone()) {
      return a;
    } else {
      return Opt.some(f.apply(a.get(), b.get()));
    }
  }

  /**
   * Apply a boolean operation to two expressions. If one of the expressions is none the other one is returned.
   */
  static Fn<EntityPath<?>, BooleanExpression> op(
          final Fn2<BooleanExpression, BooleanExpression, BooleanExpression> op,
          final Fn<EntityPath<?>, BooleanExpression> a,
          final Fn<EntityPath<?>, BooleanExpression> b) {
    return new Fn<EntityPath<?>, BooleanExpression>() {
      @Override public BooleanExpression apply(EntityPath<?> entityPath) {
        return op.apply(a.apply(entityPath), b.apply(entityPath));
      }
    };
  }

  /**
   * Combine expressions with boolean 'and' operation while removing all duplicate expressions.
   *
   * @return a combined expression or null, if the iterable is empty
   */
  @Nullable
  static BooleanExpression allOf(Iterable<BooleanExpression> expressions) {
    return Expressions.allOf(Collections.toArray(BooleanExpression.class, $(expressions).toSet()));
  }

  /**
   * Combine expressions with boolean 'and' operation while removing all duplicate expressions.
   *
   * @return a combined expression or null, if the passed array is empty or all expressions are none
   */
  @SafeVarargs
  @Nullable
  static BooleanExpression allOf(Opt<BooleanExpression>... expressions) {
    return allOf($(expressions).bind(Fns.<Opt<BooleanExpression>>id()));
  }

  /**
   * The function's return value may be null.
   */
  @SafeVarargs
  static Fn<EntityPath<?>, BooleanExpression> allOf(final Fn<EntityPath<?>, BooleanExpression>... expressions) {
    return new Fn<EntityPath<?>, BooleanExpression>() {
      @Override public BooleanExpression apply(final EntityPath<?> entityPath) {
        return allOf($(expressions).map(new Fn<Fn<EntityPath<?>, BooleanExpression>, BooleanExpression>() {
          @Override public BooleanExpression apply(Fn<EntityPath<?>, BooleanExpression> f) {
            return f.apply(entityPath);
          }
        }));
      }
    };
  }

  /**
   * The function's return value may be null.
   */
  static Fn<EntityPath<?>, BooleanExpression> allOfF(Iterable<Fn<EntityPath<?>, BooleanExpression>> expressions) {
    return allOf(Collections.toArray(Fn.class, $(expressions).toSet()));
  }

  /**
   * Combine expressions with boolean 'or' operation while removing all duplicate expressions.
   */
  @Nullable static BooleanExpression anyOf(Iterable<BooleanExpression> expressions) {
    return Expressions.anyOf(Collections.toArray(BooleanExpression.class, $(expressions).toSet()));
  }

  /**
   * Combine expressions with boolean 'or' operation while removing all duplicate expressions.
   */
  @SafeVarargs
  @Nullable
  static BooleanExpression anyOf(Opt<BooleanExpression>... expressions) {
    return anyOf($(expressions).bind(Fns.<Opt<BooleanExpression>>id()));
  }

  /**
   * Convert expressions into an array while removing duplicates.
   */
  static Expression[] toExpressionArray(Iterable<Expression<?>> as) {
    return Collections.toArray(Expression.class, $(as).toSet());
  }

  /**
   * Convert entity paths into an array while removing duplicates.
   */
  static EntityPath[] toEntityPathArray(Iterable<EntityPath<?>> as) {
    return Collections.toArray(EntityPath.class, $(as).toSet());
  }
}
