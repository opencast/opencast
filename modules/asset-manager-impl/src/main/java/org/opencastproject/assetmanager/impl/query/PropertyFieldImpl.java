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

import static org.opencastproject.assetmanager.impl.query.PropertyPredicates.NO_VALUE;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;
import org.opencastproject.assetmanager.impl.query.DeleteQueryContribution.Where;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.ProductBuilder;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.Operator;
import com.mysema.query.types.Ops;
import com.mysema.query.types.Path;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.BooleanOperation;

import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PropertyFieldImpl<A> implements PropertyField<A>, EntityPaths {
  private static final ProductBuilder p = Products.E;

  private final PropertyFieldImpl self = this;

  private final PropertyName name;
  private final ValueType<A> mkValue;

  public PropertyFieldImpl(ValueType<A> mkValue, PropertyName name) {
    this.mkValue = mkValue;
    this.name = name;
  }

  @Override public Target target() {
    return AQueryBuilderImpl.propertyTarget(name);
  }

  @Override public PropertyName name() {
    return name;
  }

  @Override public Property mk(String mpId, A value) {
    return Property.mk(PropertyId.mk(mpId, name), mkValue.mk(value));
  }

  @Override public Predicate eq(final A right) {
    return mkPredicate(Ops.EQ, right);
  }

  @Override public Predicate eq(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate lt(final A right) {
    return mkPredicate(Ops.LT, right);
  }

  @Override public Predicate lt(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate le(A right) {
    return mkPredicate(Ops.LOE, right);
  }

  @Override public Predicate le(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate gt(A right) {
    return mkPredicate(Ops.GT, right);
  }

  @Override public Predicate gt(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate ge(A right) {
    return mkPredicate(Ops.GOE, right);
  }

  @Override public Predicate ge(PropertyField<A> right) {
    // TODO implement
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override public Predicate exists() {
    return new AbstractPredicate() {
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk().where(mkWhereDeleteBase(NO_VALUE));
      }

      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return self.contributeSelect().where(mkWhereSelectBase(NO_VALUE));
      }
    };
  }

  @Override public Predicate notExists() {
    return new AbstractPredicate() {
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        final Where whereBase = mkWhereDeleteBase(NO_VALUE);
        return DeleteQueryContribution.mk().where(new Where() {
          @Override public BooleanExpression fromSnapshot(QSnapshotDto e) {
            return whereBase.fromSnapshot(e).not();
          }

          @Override public BooleanExpression fromProperty(QPropertyDto p) {
            return p.mediaPackageId.notIn(
                    new JPASubQuery()
                            .from(p)
                            .where(whereBase.fromProperty(p))
                            .distinct()
                            .list(p.mediaPackageId));
          }
        });
      }

      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return PropertyFieldImpl.this.contributeSelect().where(mkWhereSelectBase(NO_VALUE).not());
      }
    };
  }

  @Override public Order desc() {
    // TODO implement order
    throw new UnsupportedOperationException();
  }

  @Override public Order asc() {
    // TODO implement order
    throw new UnsupportedOperationException();
  }

  /**
   * Create a predicate to compare this field's value with a constant value.
   */
  private Fn<QPropertyDto, Opt<BooleanExpression>> mkValuePredicate(final Operator<? super Boolean> op, final A value) {
    return new Fn<QPropertyDto, Opt<BooleanExpression>>() {
      @Override public Opt<BooleanExpression> apply(final QPropertyDto dto) {
        final BooleanExpression expr = mkValue.mk(value).decompose(
            new Fn<String, BooleanExpression>() {
              @Override public BooleanExpression apply(String a) {
                return BooleanOperation.create(op, dto.stringValue, ConstantImpl.create(a));
              }
            }, new Fn<Date, BooleanExpression>() {
              @Override public BooleanExpression apply(Date a) {
                return BooleanOperation.create(op, dto.dateValue, ConstantImpl.create(a));
              }
            }, new Fn<Long, BooleanExpression>() {
              @Override public BooleanExpression apply(Long a) {
                return BooleanOperation.create(op, dto.longValue, ConstantImpl.create(a));
              }
            }, new Fn<Boolean, BooleanExpression>() {
              @Override public BooleanExpression apply(Boolean a) {
                return BooleanOperation.create(op, dto.boolValue, ConstantImpl.create(a));
              }
            }, new Fn<Version, BooleanExpression>() {
              @Override public BooleanExpression apply(Version a) {
                return BooleanOperation.create(op, dto.longValue, ConstantImpl.create(RuntimeTypes.convert(a).value()));
              }
            });
        return Opt.some(expr);
      }
    };
  }

  public Path<?> getPath(QPropertyDto dto) {
    return mkValue.match(
            p.p1(dto.stringValue),
            p.p1(dto.dateValue),
            p.p1(dto.longValue),
            p.p1(dto.boolValue),
            p.p1(dto.longValue));
  }

//  /**
//   * Create a predicate to compare this field's value with the value of another property.
//   */
//  private Fn<QPropertyDto, Opt<BooleanExpression>> mkValuePredicate(
//      final Operator<? super Boolean> op,
//      final PropertyField<A> prop) {
//    return new Fn<QPropertyDto, Opt<BooleanExpression>>() {
//      @Override public Opt<BooleanExpression> apply(final QPropertyDto dto) {
//        final BooleanExpression expr = mkValue.match(new Fn<StringType, BooleanExpression>() {
//          @Override public BooleanExpression apply(StringType stringType) {
//            return BooleanOperation.create(op, dto.stringValue, PropertyPredicates.)
//          }
//        }, new Fn<DateType, BooleanExpression>() {
//          @Override public BooleanExpression apply(DateType dateType) {
//            return null;
//          }
//        }, new Fn<LongType, BooleanExpression>() {
//          @Override public BooleanExpression apply(LongType longType) {
//            return null;
//          }
//        }, new Fn<BooleanType, BooleanExpression>() {
//          @Override public BooleanExpression apply(BooleanType booleanType) {
//            return null;
//          }
//        });
//        return Opt.some(expr);
//      }
//    };
//  }


  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Create a predicate that compares the property (of this field) with the given constant value.
   */
  private Predicate mkPredicate(final Operator<? super Boolean> op, final A value) {
    return new AbstractPredicate() {
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        return DeleteQueryContribution.mk().where(mkWhereDeleteBase(mkValuePredicate(op, value)));
      }

      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
        return self.contributeSelect().where(mkWhereSelectBase(mkValuePredicate(op, value)));
      }
    };
  }

//  /**
//   * Create a predicate that compares the property (of this field) with the given property.
//   */
//  private Predicate mkPredicate(final Operator<? super Boolean> op, final PropertyField<A> value) {
//    return new AbstractPredicate() {
//      @Override public DeleteQueryContribution contributeDelete() {
//        return DeleteQueryContribution.mk().where(mkWhereDeleteBase(mkValuePredicate(op, value)));
//      }
//
//      @Override public SelectQueryContribution contributeSelect(JPAQueryFactory f) {
//        return PropertyFieldImpl.this.contributeSelect().where(mkWhereSelectBase(mkValuePredicate(op, value)));
//      }
//    };
//  }

  /**
   * Create the base contribution for selects.
   */
  private SelectQueryContribution contributeSelect() {
    return SelectQueryContribution.mk();
  }

  /**
   * Create the base expression for the where clause of a delete.
   *
   * @param valueExpression a function that creates a property value expression
   */
  private Where mkWhereDeleteBase(final Fn<QPropertyDto, Opt<BooleanExpression>> valueExpression) {
    return PropertyPredicates.mkWhereDelete(name, valueExpression);
  }

  /**
   * Create the base expression for the where clause of a select.
   *
   * @param valueExpression a function that creates a property value expression
   */
  private BooleanExpression mkWhereSelectBase(Fn<QPropertyDto, Opt<BooleanExpression>> valueExpression) {
    return PropertyPredicates.mkWhereSelect(name, valueExpression);
  }
}
