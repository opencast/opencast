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

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.expr.BooleanExpression;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Collect contributions to a JPA query.
 * Each of the builder methods creates a new instance.
 */
@ParametersAreNonnullByDefault
public final class SelectQueryContribution {
  // CHECKSTYLE:OFF
  final Stream<Expression<?>> fetch;
  final Stream<EntityPath<?>> from;
  final Stream<Join> join;
  final Opt<BooleanExpression> where;
  final Opt<Integer> offset;
  final Opt<Integer> limit;
  final Stream<OrderSpecifier<?>> order;
  // CHECKSTYLE:ON

  public SelectQueryContribution(
          Stream<Expression<?>> fetch,
          Stream<EntityPath<?>> from,
          Stream<Join> join,
          Opt<BooleanExpression> where,
          Opt<Integer> offset,
          Opt<Integer> limit,
          Stream<OrderSpecifier<?>> order) {
    this.fetch = fetch;
    this.from = from;
    this.join = join;
    this.where = where;
    this.offset = offset;
    this.limit = limit;
    this.order = order;
  }

  /** Create a new, empty contribution. */
  public static SelectQueryContribution mk() {
    return new SelectQueryContribution(Stream.<Expression<?>>empty(), Stream.<EntityPath<?>>empty(), Stream.<Join>empty(), Opt.<BooleanExpression>none(), Opt.<Integer>none(), Opt.<Integer>none(), Stream.<OrderSpecifier<?>>empty());
  }

  /** Set the `fetch` contribution. */
  SelectQueryContribution fetch(Stream<? extends Expression<?>> fetch) {
    return new SelectQueryContribution((Stream<Expression<?>>) fetch, from, join, where, offset, limit, order);
  }

  /** Add to the `from` contribution. */
  SelectQueryContribution addFetch(Stream<? extends Expression<?>> fetch) {
    return new SelectQueryContribution(this.fetch.append(fetch), from, join, where, offset, limit, order);
  }

  /** Set the `from` contribution. */
  SelectQueryContribution from(Stream<? extends EntityPath<?>> from) {
    return new SelectQueryContribution(fetch, (Stream<EntityPath<?>>) from, join, where, offset, limit, order);
  }

  /** Add to the `from` contribution. */
  SelectQueryContribution addFrom(Stream<? extends EntityPath<?>> from) {
    return new SelectQueryContribution(fetch, this.from.append(from), join, where, offset, limit, order);
  }

  /** Set the `join` contribution. */
  SelectQueryContribution join(Stream<Join> join) {
    return new SelectQueryContribution(fetch, from, join, where, offset, limit, order);
  }

  /** Set the `join` contribution. */
  SelectQueryContribution join(Join join) {
    return join($(join));
  }

  /** Add to the `join` contribution. */
  SelectQueryContribution addJoin(Stream<Join> join) {
    return new SelectQueryContribution(fetch, from, this.join.append(join), where, offset, limit, order);
  }

  /** Add to the `join` contribution. */
  SelectQueryContribution addJoin(Join join) {
    return addJoin($(join));
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Set the `where` contribution. */
  SelectQueryContribution where(Opt<BooleanExpression> where) {
    return new SelectQueryContribution(fetch, from, join, where, offset, limit, order);
  }

  /** Set the `where` contribution. */
  SelectQueryContribution where(@Nullable BooleanExpression where) {
    return where(Opt.nul(where));
  }

  /** Add to the `where` contribution using boolean "and". */
  SelectQueryContribution andWhere(Opt<BooleanExpression> where) {
    return where(JpaFns.allOf(this.where, where));
  }

  /** Add to the `where` contribution using boolean "and". */
  SelectQueryContribution andWhere(@Nullable BooleanExpression where) {
    return andWhere(Opt.nul(where));
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Set the `offset` contribution. */
  SelectQueryContribution offset(Opt<Integer> offset) {
    return new SelectQueryContribution(fetch, from, join, where, offset, limit, order);
  }

  /** Set the `offset` contribution. */
  SelectQueryContribution offset(Integer offset) {
    return offset(Opt.some(offset));
  }

  /** Set the `limit` contribution. */
  SelectQueryContribution limit(Opt<Integer> limit) {
    return new SelectQueryContribution(fetch, from, join, where, offset, limit, order);
  }

  /** Set the `limit` contribution. */
  SelectQueryContribution limit(Integer limit) {
    return limit(Opt.some(limit));
  }

  /** Set the `order` contribution. */
  SelectQueryContribution order(Stream<? extends OrderSpecifier<?>> order) {
    return new SelectQueryContribution(fetch, from, join, where, offset, limit, (Stream<OrderSpecifier<?>>) order);
  }

  /** Add to the `order` contribution. */
  SelectQueryContribution addOrder(Stream<? extends OrderSpecifier<?>> order) {
    return new SelectQueryContribution(fetch, from, join, where, offset, limit, this.order.append(order));
  }

  static final Fn<SelectQueryContribution, Stream<Expression<?>>> getFetch = new Fn<SelectQueryContribution, Stream<Expression<?>>>() {
    @Override public Stream<Expression<?>> apply(SelectQueryContribution c) {
      return c.fetch;
    }
  };

  static final Fn<SelectQueryContribution, Stream<EntityPath<?>>> getFrom = new Fn<SelectQueryContribution, Stream<EntityPath<?>>>() {
    @Override public Stream<EntityPath<?>> apply(SelectQueryContribution c) {
      return c.from;
    }
  };

  static final Fn<SelectQueryContribution, Stream<Join>> getJoin = new Fn<SelectQueryContribution, Stream<Join>>() {
    @Override public Stream<Join> apply(SelectQueryContribution c) {
      return c.join;
    }
  };

  static final Fn<SelectQueryContribution, Opt<BooleanExpression>> getWhere = new Fn<SelectQueryContribution, Opt<BooleanExpression>>() {
    @Override public Opt<BooleanExpression> apply(SelectQueryContribution c) {
      return c.where;
    }
  };

  static final Fn<SelectQueryContribution, Stream<OrderSpecifier<?>>> getOrder = new Fn<SelectQueryContribution, Stream<OrderSpecifier<?>>>() {
    @Override public Stream<OrderSpecifier<?>> apply(SelectQueryContribution c) {
      return c.order;
    }
  };
}
