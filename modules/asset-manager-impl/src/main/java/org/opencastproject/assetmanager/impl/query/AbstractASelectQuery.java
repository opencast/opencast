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

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.assetmanager.impl.persistence.PropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.SnapshotDto;
import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.SetB;
import com.entwinemedia.fn.fns.Booleans;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.expr.BooleanExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractASelectQuery implements ASelectQuery, SelectQueryContributor, EntityPaths {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractASelectQuery.class);

  private final AbstractASelectQuery self = this;
  private final AbstractAssetManager am;

  public AbstractASelectQuery(AbstractAssetManager am) {
    this.am = am;
  }

  @Override public ASelectQuery where(final Predicate predicate) {
    return new AbstractASelectQuery(am) {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        final SelectQueryContribution predicateContrib = RuntimeTypes.convert(predicate).contributeSelect(f);
        return self.contributeSelect(f)
                .addFrom(predicateContrib.from)
                .addJoin(predicateContrib.join)
                .andWhere(predicateContrib.where);
      }

      @Override public String toString() {
        return "where " + predicate;
      }
    };
  }

  @Override public ASelectQuery page(final int offset, final int size) {
    return new AbstractASelectQuery(am) {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return self.contributeSelect(f).offset(offset).limit(size);
      }
    };
  }

  @Override public ASelectQuery orderBy(final Order order) {
    return new AbstractASelectQuery(am) {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        final SelectQueryContribution orderContrib = RuntimeTypes.convert(order).contributeSelect(f);
        return self.contributeSelect(f).addOrder(orderContrib.order).andWhere(orderContrib.where);
      }
    };
  }

  @Override public AResult run() {
    return am.getDb().run(new Fn<JPAQueryFactory, AResult>() {
      @Override public AResult apply(JPAQueryFactory f) {
        return run(f);
      }
    });
  }

  private AResult run(JPAQueryFactory f) {
    // run query and map the result to records
    final long startTime = System.nanoTime();
    // resolve AST
    final SelectQueryContribution r = contributeSelect(f);
    final boolean toFetchProperties = r.fetch.exists(Booleans.<Expression<?>>eq(QPropertyDto.propertyDto));
    // # create Querydsl query
    final JPAQuery q = f.query();
    // # from
    {
      // Make sure that the snapshotDto is always contained in the from clause because the media package ID and
      //   the ID are always selected.
      // Use a mutable hash set to be able to use the removeAll operation.
      final Set<EntityPath<?>> from = Stream.<EntityPath<?>>mk(Q_SNAPSHOT)
              .append(r.from) // all collected from clauses
              .append(r.join.map(Join.getFrom)) // all from clauses from the joins
              .toSet(SetB.MH);
      // Now remove everything that will be joined. Adding them in both the from and a join
      //   clause is not allowed.
      from.removeAll(r.join.map(Join.getJoin).toSet());
      q.from(JpaFns.toEntityPathArray(from));
    }
    // # join
    if (!r.join.isEmpty()) {
      // Group joins by entity and combine all "on" clauses with "or" expressions.
      // This way there is only one join clause per distinct entity which eliminates the need to alias entities
      //   like this `new QPropertyDto("alias")`.
      // Entity aliasing produces many issues which seem to cause a huge rewrite of the query building mechanism
      //   so it should be prevented at all costs.
      final Map<EntityPath<?>, BooleanExpression> joins = r.join.foldl(
          new HashMap<EntityPath<?>, BooleanExpression>(),
          new Fn2<Map<EntityPath<?>, BooleanExpression>, Join, Map<EntityPath<?>, BooleanExpression>>() {
            @Override
            public Map<EntityPath<?>, BooleanExpression> apply(Map<EntityPath<?>, BooleanExpression> sum, Join join) {
              // get the on expression saved with the join, may be null
              final BooleanExpression existing = sum.get(join.join);
              final BooleanExpression combined;
              // combine the existing and the current expression
              if (existing == null) {
                combined = join.on;
              } else if (existing.equals(join.on)) {
                // if both expressions are equal there is no need to combine them
                combined = existing;
              } else {
                // if different combine with logical "or"
                combined = existing.or(join.on);
              }
              sum.put(join.join, combined);
              return sum;
            }
          });
      for (final Map.Entry<EntityPath<?>, BooleanExpression> j : joins.entrySet()) {
        q.leftJoin(j.getKey()).on(j.getValue());
      }
    }
    // # where
    q.where(r.where.orNull());
    // # paging
    for (Integer a : r.offset) {
      q.offset(a);
    }
    for (Integer a : r.limit) {
      q.limit(a);
    }
    // # order
    for (OrderSpecifier<?> a : r.order) {
      q.orderBy(a);
    }
    // # distinct
    if (!toFetchProperties) {
      // if no properties shall be fetched the result set can be distinct
      q.distinct();
    }
    // # fetch
    // create parameters for fetch clause, i.e. Querydsl's list() method
    final List<Expression<?>> fetch;
    {
      // check if the media package ID needs to be selected separately
      if (r.fetch.exists(MandatoryFetch.exists)) {
        fetch = r.fetch.toList();
      } else {
        fetch = r.fetch.append(MandatoryFetch.fetch).toList();
      }
    }
    // Run the query and transform the result into records
    final Stream<ARecordImpl> records;
    {
      // run query
      am.getDb().logQuery(q);
      final List<Tuple> result = q.list(JpaFns.toExpressionArray(fetch));
      logger.debug("Pure query ms " + (System.nanoTime() - startTime) / 1000000);
      // map result based on the fact whether properties have been fetched or not
      if (!toFetchProperties) {
        // No properties have been fetched -> each result row (tuple) is a distinct record (snapshot).
        records = $($(result).map(toARecord(r))).map(new Fn<ARecordImpl, ARecordImpl>() {
          @Override
          public ARecordImpl apply(ARecordImpl record) {
            Opt<Snapshot> snapshotOpt = record.getSnapshot();
            Snapshot snapshot = null;
            if (snapshotOpt.isSome()) {
              // make sure the delivered media package has valid URIs
              snapshot = am.getHttpAssetProvider().prepareForDelivery(snapshotOpt.get());
            }
            return new ARecordImpl(
                    record.getSnapshotId(),
                    record.getMediaPackageId(),
                    record.getProperties(),
                    snapshot);
          }
        });
      } else {
        logger.trace("Fetched properties");
        // Properties have been fetched -> there may be multiple rows (tuples)
        // per snapshot because of the join with the property table. Extract
        // records and properties and link them together.

        // group properties after their media package ID and make sure that no duplicate properties occur
        final Map<String, Set<Property>> propertiesPerMp = $(result).bind(toProperty).foldl(
            new HashMap<String, Set<Property>>(),
            new Fn2<Map<String, Set<Property>>, Property, Map<String, Set<Property>>>() {
              @Override
              public Map<String, Set<Property>> apply(Map<String, Set<Property>> sum, Property p) {
                final String mpId = p.getId().getMediaPackageId();
                final Set<Property> props = sum.get(mpId);
                if (props != null) {
                  props.add(p);
                } else {
                  sum.put(mpId, SetB.MH.mk(p));
                }
                return sum;
              }
            });
        // group records after their media package ID
        final Map<String, List<ARecordImpl>> distinctRecords = $($(result)
            .map(toARecord(r)).toSet())
            .groupMulti(ARecordImpl.getMediaPackageId);
        records = $(distinctRecords.values()).bind(new Fn<List<ARecordImpl>, Iterable<ARecordImpl>>() {
          @Override public Iterable<ARecordImpl> apply(List<ARecordImpl> records) {
            return $(records).map(new Fn<ARecordImpl, ARecordImpl>() {
              @Override public ARecordImpl apply(ARecordImpl record) {
                final Set<Property> properties = propertiesPerMp.get(record.getMediaPackageId());
                final Stream<Property> p = properties != null ? $(properties) : Stream.<Property>empty();
                Snapshot snapshot = null;
                Opt<Snapshot> snapshotOpt = record.getSnapshot();
                if (snapshotOpt.isSome()) {
                  // make sure the delivered media package has valid URIs
                  snapshot = am.getHttpAssetProvider().prepareForDelivery(snapshotOpt.get());
                }
                return new ARecordImpl(record.getSnapshotId(), record.getMediaPackageId(), p, snapshot);
              }
            });
          }
        });
      }
    }
    final long searchTime = (System.nanoTime() - startTime) / 1000000;
    logger.debug("Complete query ms " + searchTime);
    return new AResultImpl(
        AbstractASelectQuery.<ARecord>vary(records),
        sizeOf(records),
        r.offset.getOr(0),
        r.limit.getOr(-1),
        searchTime
    );
  }

  /**
   * Transform a Querydsl result {@link Tuple} into an {@link ARecord}.
   * To do the transformation I need to know what targets have been selected.
   */
  private Fn<Tuple, ARecordImpl> toARecord(final SelectQueryContribution c) {
    return new Fn<Tuple, ARecordImpl>() {
      @Override public ARecordImpl apply(Tuple tuple) {
        final String mediaPackageId;
        SnapshotDto snapshotDto = null;
        final long id;
        // Only fetch the snapshot if it is in the fetch list.
        if (c.fetch.exists(Booleans.<Expression<?>>eq(Q_SNAPSHOT))) {
          snapshotDto = RequireUtil.notNull(tuple.get(Q_SNAPSHOT), "[BUG] snapshot table data");
          id = snapshotDto.getId();
          mediaPackageId = snapshotDto.getMediaPackageId();
        } else {
          // The media package ID and the snapshot's database ID must always be fetched.
          id = RequireUtil.notNull(tuple.get(Q_SNAPSHOT.id), "[BUG] snapshot table id");
          mediaPackageId = RequireUtil.notNull(
              tuple.get(Q_SNAPSHOT.mediaPackageId),
              "[BUG] snapshot table media package id"
          );
        }
        return new ARecordImpl(id, mediaPackageId, Stream.<Property>empty(), snapshotDto);
      }
    };
  }

  private static Fn<Tuple, Opt<Property>> toProperty = new Fn<Tuple, Opt<Property>>() {
    @Override public Opt<Property> apply(Tuple tuple) {
      final PropertyDto dto = tuple.get(Q_PROPERTY);
      return dto != null ? Opt.some(dto.toProperty()) : Opt.<Property>none();
    }
  };

  /**
   * Specification of fields whose fetch is mandatory.
   */
  private static final class MandatoryFetch {
    static final Fn<Expression<?>, Boolean> exists =
            Booleans.<Expression<?>>eq(Q_SNAPSHOT)
                    .or(Booleans.<Expression<?>>eq(Q_SNAPSHOT.mediaPackageId))
                    .or(Booleans.<Expression<?>>eq(Q_SNAPSHOT.id));

    static final Stream<Expression<?>> fetch = Stream.<Expression<?>>mk(Q_SNAPSHOT.mediaPackageId, Q_SNAPSHOT.id);
  }

  private static <A> Stream<A> vary(Stream<? extends A> a) {
    return (Stream<A>) a;
  }

  private static <A> int sizeOf(Stream<A> stream) {
    int count = 0;
    for (A ignore : stream) {
      count++;
    }
    return count;
  }
}
