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

import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Collect contributions to a JPA query.
 * Each of the builder methods creates a new instance.
 */
@ParametersAreNonnullByDefault
public final class DeleteQueryContribution {
  // CHECKSTYLE:OFF
  final Stream<EntityPath<?>> from;

  /**
   * Where clause constructor function.
   * <code>(EntityPath from) -> (BooleanExpression | null)</code>
   */
  final Fn<EntityPath<?>, BooleanExpression> where;

  final Opt<BooleanExpression> targetPredicate;

  final String name;

  private static final Fn<EntityPath<?>, BooleanExpression> NO_WHERE = new Fn<EntityPath<?>, BooleanExpression>() {
    @Override public BooleanExpression apply(EntityPath<?> entityPathBase) {
      return null;
    }
  };
  // CHECKSTYLE:ON

  public DeleteQueryContribution(
          Stream<EntityPath<?>> from,
          Fn<EntityPath<?>, BooleanExpression> where,
          Opt<BooleanExpression> targetPredicate,
          String name) {
    this.from = from;
    this.where = where;
    this.targetPredicate = targetPredicate;
    this.name = name;
  }

  /**
   * Create an empty contribution.
   */
  public static DeleteQueryContribution mk() {
    return new DeleteQueryContribution(Stream.<EntityPath<?>>empty(), NO_WHERE, Opt.<BooleanExpression>none(), "");
  }

  /**
   * Create a copy of contribution <code>c</code>.
   */
  public static DeleteQueryContribution mk(DeleteQueryContribution c) {
    return new DeleteQueryContribution(c.from, c.where, c.targetPredicate, c.name);
  }

  DeleteQueryContribution from(Stream<? extends EntityPath<?>> from) {
    return new DeleteQueryContribution((Stream<EntityPath<?>>) from, where, targetPredicate, name);
  }

  DeleteQueryContribution targetPredicate(@Nullable BooleanExpression targetPredicate) {
    return new DeleteQueryContribution(from, where, Opt.nul(targetPredicate), name);
  }

  DeleteQueryContribution targetPredicate(Opt<BooleanExpression> targetPredicate) {
    return new DeleteQueryContribution(from, where, targetPredicate, name);
  }

  DeleteQueryContribution where(Fn<EntityPath<?>, BooleanExpression> where) {
    return new DeleteQueryContribution(from, where, targetPredicate, name);
  }

  DeleteQueryContribution where(final Where where) {
    return new DeleteQueryContribution(from, toFn(where), targetPredicate, name);
  }

  DeleteQueryContribution where(@Nullable final BooleanExpression where) {
    final Fn<EntityPath<?>, BooleanExpression> w = new Fn<EntityPath<?>, BooleanExpression>() {
      @Override public BooleanExpression apply(EntityPath<?> entityPath) {
        return where;
      }
    };
    return new DeleteQueryContribution(from, w, targetPredicate, name);
  }

  DeleteQueryContribution name(String name) {
    return new DeleteQueryContribution(from, where, targetPredicate, name);
  }

  /* -- */

  static Fn<EntityPath<?>, BooleanExpression> toFn(final Where where) {
    return new Fn<EntityPath<?>, BooleanExpression>() {
      @Override public BooleanExpression apply(EntityPath<?> from) {
        if (from instanceof QSnapshotDto) {
          return where.fromSnapshot((QSnapshotDto) from);
        } else if (from instanceof QPropertyDto) {
          return where.fromProperty((QPropertyDto) from);
        } else {
          throw new RuntimeException("BUG");
        }
      }
    };
  }

  static final Fn<DeleteQueryContribution, Stream<EntityPath<?>>> getFrom = new Fn<DeleteQueryContribution, Stream<EntityPath<?>>>() {
    @Override public Stream<EntityPath<?>> apply(DeleteQueryContribution c) {
      return c.from;
    }
  };

  static final Fn<DeleteQueryContribution, Fn<EntityPath<?>, BooleanExpression>> getWhere = new Fn<DeleteQueryContribution, Fn<EntityPath<?>, BooleanExpression>>() {
    @Override public Fn<EntityPath<?>, BooleanExpression> apply(DeleteQueryContribution c) {
      return c.where;
    }
  };

  /* -- */

  @ParametersAreNonnullByDefault
  interface Where {
    BooleanExpression fromSnapshot(QSnapshotDto e);
    BooleanExpression fromProperty(QPropertyDto p);
  }
}
