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

import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.impl.RuntimeTypes;

import com.entwinemedia.fn.Fn2;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.types.expr.BooleanExpression;

public abstract class AbstractPredicate implements Predicate, QueryContributor {
  private final AbstractPredicate self = this;

  /**
   * Join two predicates by <code>op</code>.
   */
  private Predicate binaryOp(
      final Predicate right,
      final Fn2<BooleanExpression, BooleanExpression, BooleanExpression> op
  ) {
    return new AbstractPredicate() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        final SelectQueryContribution cLeft = self.contributeSelect(f);
        final SelectQueryContribution cRight = RuntimeTypes.convert(right).contributeSelect(f);
        return SelectQueryContribution.mk()
                .from(cLeft.from.append(cRight.from))
                .join(cLeft.join.append(cRight.join))
                .where(JpaFns.op(op, cLeft.where, cRight.where));
      }

      @Override public DeleteQueryContribution contributeDelete(String owner) {
        final DeleteQueryContribution cLeft = self.contributeDelete(owner);
        final DeleteQueryContribution cRight = RuntimeTypes.convert(right).contributeDelete(owner);
        return DeleteQueryContribution.mk()
                .from(cLeft.from.append(cRight.from))
                .where(JpaFns.op(op, cLeft.where, cRight.where));
      }
    };
  }

  @Override public Predicate and(final Predicate right) {
    return binaryOp(right, JpaFns.and);
  }

  @Override public Predicate or(final Predicate right) {
    return binaryOp(right, JpaFns.or);
  }

  @Override public Predicate not() {
    return new AbstractPredicate() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        final SelectQueryContribution c = self.contributeSelect(f);
        return SelectQueryContribution.mk().from(c.from).join(c.join).where(c.where.map(JpaFns.not));
      }

      @Override public DeleteQueryContribution contributeDelete(String owner) {
        final DeleteQueryContribution c = self.contributeDelete(owner);
        return DeleteQueryContribution.mk().from(c.from).where(c.where.then(JpaFns.not));
      }
    };
  }
}
