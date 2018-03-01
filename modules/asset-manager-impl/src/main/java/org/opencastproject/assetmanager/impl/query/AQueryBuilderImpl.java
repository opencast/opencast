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
import static org.opencastproject.assetmanager.impl.query.PropertyPredicates.NONE;
import static org.opencastproject.assetmanager.impl.query.PropertyPredicates.NO_VALUE;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Field;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;
import org.opencastproject.assetmanager.impl.query.DeleteQueryContribution.Where;
import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.types.expr.BooleanExpression;

import java.util.Date;

import javax.annotation.Nonnull;

public final class AQueryBuilderImpl implements AQueryBuilder, EntityPaths {
  private static final Stream<QSnapshotDto> FROM_SNAPSHOT = $Q_SNAPSHOT;

  private final AbstractAssetManager am;

  public AQueryBuilderImpl(AbstractAssetManager am) {
    this.am = am;
  }

  /** Convert a {@link Target} into a Querydsl {@link com.mysema.query.types.Expression}. */
  private static Fn<Target, SelectQueryContribution> contributeSelect(final JPAQueryFactory f) {
    return new Fn<Target, SelectQueryContribution>() {
      @Override public SelectQueryContribution apply(Target t) {
        return RuntimeTypes.convert(t).contributeSelect(f);
      }
    };
  }

  @Override public ASelectQuery select(final Target... target) {
    return new AbstractASelectQuery(am) {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        final Stream<SelectQueryContribution> c = $(target).map(AQueryBuilderImpl.contributeSelect(f));
        return SelectQueryContribution.mk()
                .fetch(c.bind(SelectQueryContribution.getFetch))
                .from(c.bind(SelectQueryContribution.getFrom))
                .join(c.bind(SelectQueryContribution.getJoin))
                .where(Opt.nul(JpaFns.allOf(c.bind(SelectQueryContribution.getWhere))));
      }
    };
  }

  @Override public ADeleteQuery delete(final String owner, final Target target) {
    RequireUtil.notEmpty(owner, "owner");
    return new AbstractADeleteQuery(am, owner) {
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        final DeleteQueryContribution c = RuntimeTypes.convert(target).contributeDelete(owner);
        return DeleteQueryContribution.mk()
                .from(c.from)
                .targetPredicate(c.targetPredicate)
                .where(c.where);
//                .where(new Fn<EntityPath<?>, BooleanExpression>() {
//                  @Override public BooleanExpression apply(EntityPath<?> path) {
//                    // Wildcard deletion. Disabled as of ticket CERV-1158. Kept for potentially later reference.
//                    // return !"".equals(owner) ? Q_SNAPSHOT.owner.eq(owner).and(c.where.apply(path)) : c.where.apply(path);
//                    return Q_SNAPSHOT.owner.eq(owner).and(c.where.apply(path));
//                  }
//                });
      }
    };
  }

  /* -- */

  @Override public Predicate mediaPackageId(final String mpId) {
    return new AbstractPredicate() {
      /* SELECT */
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk().from(FROM_SNAPSHOT).where(Q_SNAPSHOT.mediaPackageId.eq(mpId));
      }

      /* DELETE */
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk().where(new Where() {
          @Override public BooleanExpression fromSnapshot(@Nonnull QSnapshotDto e) {
            return e.mediaPackageId.eq(mpId);
          }

          @Override public BooleanExpression fromProperty(@Nonnull QPropertyDto p) {
            return p.mediaPackageId.eq(mpId);
          }
        });
      }

    };
  }

  /**
   * A predicate that is based on a simple snapshot field expression.
   */
  private abstract static class SnapshotBasedPredicate extends AbstractPredicate {
    /* SELECT */
    @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
      return SelectQueryContribution.mk().from(FROM_SNAPSHOT).where(mkSnapshotFieldPredicate(Q_SNAPSHOT));
    }

    /* DELETE */
    @Override public DeleteQueryContribution contributeDelete(String owner) {
      return DeleteQueryContribution.mk().where(new Where() {
        @Override public BooleanExpression fromSnapshot(@Nonnull QSnapshotDto e) {
          return mkSnapshotFieldPredicate(e);
        }

        @Override public BooleanExpression fromProperty(@Nonnull QPropertyDto p) {
          return mkSnapshotFieldPredicate(Q_SNAPSHOT);
        }
      });
    }

    protected abstract BooleanExpression mkSnapshotFieldPredicate(QSnapshotDto e);
  }

  @Override public Field<String> seriesId() {
    return new SimpleSnapshotField<>(Q_SNAPSHOT.seriesId);
  }

  @Override public Predicate organizationId(final String orgId) {
    return new SnapshotBasedPredicate() {
      @Override protected BooleanExpression mkSnapshotFieldPredicate(QSnapshotDto e) {
        return e.organizationId.eq(orgId);
      }
    };
  }

  @Override public Field<String> organizationId() {
    return new SimpleSnapshotField<>(Q_SNAPSHOT.organizationId);
  }

  @Override public Field<String> owner() {
    return new SimpleSnapshotField<>(Q_SNAPSHOT.owner);
  }

  @Override public Predicate availability(final Availability availability) {
    return new SnapshotBasedPredicate() {
      @Override protected BooleanExpression mkSnapshotFieldPredicate(QSnapshotDto e) {
        return e.availability.eq(availability.name());
      }
    };
  }

  @Override public Predicate storage(final String storage) {
    return new SnapshotBasedPredicate() {
      @Override protected BooleanExpression mkSnapshotFieldPredicate(QSnapshotDto e) {
        return e.storageId.eq(storage);
      }
    };
  }

  @Override public Field<Availability> availability() {
    return new AbstractSnapshotField<Availability, String>(Q_SNAPSHOT.availability) {
      @Override protected String extract(Availability availability) {
        return availability.name();
      }
    };
  }

  @Override public Field<String> storage() {
    return new AbstractSnapshotField<String, String>(Q_SNAPSHOT.availability) {
      @Override protected String extract(String storageId) {
        return storageId;
      }
    };
  }

  /* -- */

  // TODO DRY with #hasProperties
  @Override public Predicate hasPropertiesOf(final String namespace) {
    return new AbstractPredicate() {
      /* SELECT */
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk()
                .where(PropertyPredicates.mkWhereSelect(Opt.some(namespace), NONE, NO_VALUE));
      }

      /* DELETE */
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk()
                .where(PropertyPredicates.mkWhereDelete(Opt.some(namespace), NONE, NO_VALUE));
      }
    };
  }

  // TODO DRY with #hasPropertiesOf
  @Override public Predicate hasProperties() {
    return new AbstractPredicate() {
      /* SELECT */
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk()
                .where(PropertyPredicates.mkWhereSelect(NONE, NONE, NO_VALUE));
      }

      /* DELETE */
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk()
                .where(PropertyPredicates.mkWhereDelete(NONE, NONE, NO_VALUE));
      }
    };
  }

  @Override public Field<Date> archived() {
    return new SimpleSnapshotField<>(Q_SNAPSHOT.archivalDate);
  }

  @Override public VersionField version() {
    return new VersionFieldImpl();
  }

  @Override public <A> PropertyField<A> property(ValueType<A> ev, String namespace, String name) {
    return new PropertyFieldImpl<>(ev, PropertyName.mk(namespace, name));
  }

  @Override public <A> PropertyField<A> property(ValueType<A> ev, PropertyName fqn) {
    return new PropertyFieldImpl<>(ev, fqn);
  }

  @Override public Target snapshot() {
    return new AbstractTarget() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk().from(FROM_SNAPSHOT).fetch($Q_SNAPSHOT);
      }

      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk().from(FROM_SNAPSHOT).where(Q_SNAPSHOT.owner.eq(owner));
      }
    };
  }

  @Override public Target propertiesOf(final String... namespace) {
    return propertyTarget(namespace);
  }

  @Override public Target properties(PropertyName... fqn) {
    return propertyTarget(fqn);
  }

  @Override public Target nothing() {
    return new AbstractTarget() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return SelectQueryContribution.mk();
      }

      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk();
      }

    };
  }

  @Override public Field zero() {
    // TODO implement zero element of fields
    throw new UnsupportedOperationException();
  }

  @Override public Predicate always() {
    return new AbstractPredicate() {
      /* SELECT */
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        // could not find a boolean expression being constantly true, so use this as a workaround
        return SelectQueryContribution.mk().where(Q_SNAPSHOT.eq(Q_SNAPSHOT));
      }

      /* DELETE */
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        // could not find a boolean expression being constantly true, so use this as a workaround
        return DeleteQueryContribution.mk().where(Q_SNAPSHOT.eq(Q_SNAPSHOT));
      }
    };
  }

  //
  //
  //

  static Target propertyTarget(String... namespace) {
    final Stream<BooleanExpression> onExpressions = $(namespace).map(new Fn<String, BooleanExpression>() {
      @Override public BooleanExpression apply(String namespace) {
        return Q_PROPERTY.namespace.eq(namespace);
      }
    });
    return propertyTarget(onExpressions);
  }

  static Target propertyTarget(PropertyName... fqn) {
    final Stream<BooleanExpression> onExpressions = $(fqn).map(new Fn<PropertyName, BooleanExpression>() {
      @Override public BooleanExpression apply(PropertyName name) {
        return Q_PROPERTY.namespace.eq(name.getNamespace()).and(Q_PROPERTY.propertyName.eq(name.getName()));
      }
    });
    return propertyTarget(onExpressions);
  }

  /**
   * Create a property target using the given, additional expressions for the join's on clause.
   * On expressions are combined with logical "or".
   */
  private static Target propertyTarget(final Stream<BooleanExpression> onExpressions) {
    return new AbstractTarget() {
      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        // join on the media package ID and the given expressions
        final BooleanExpression on = Q_PROPERTY.mediaPackageId.eq(Q_SNAPSHOT.mediaPackageId).and(JpaFns.anyOf(onExpressions));
        return SelectQueryContribution.mk().join($(new Join(Q_SNAPSHOT, Q_PROPERTY, on))).fetch($Q_PROPERTY);
      }

      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk().from($Q_PROPERTY).targetPredicate(JpaFns.anyOf(onExpressions));
      }
    };
  }
}
