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
package org.opencastproject.assetmanager.impl;

import static com.entwinemedia.fn.Stream.$;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.Values;
import org.opencastproject.assetmanager.api.fn.ARecords;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.fns.Booleans;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class AbstractAssetManagerPropertyRetrievalTest extends AbstractAssetManagerTestBase {
  /**
   * Create some media packages and associate some random properties to each of them.
   * Then iterate all created properties and create a query for each of them.
   */
  @Test
  @Parameters
  public void testPropertyRetrieval(final Params params) {
    // create a set of media packages and add them to the asset manager
    final String[] mps = createAndAddMediaPackagesSimple(params.mpCount, 1, 1);
    final Random random = new Random(System.nanoTime());
    // create a set of random property names
    final PropertyName[] propertyNames = createRandomPropertyNames(params.propertyNameSetSize);
    // create a random amount of random properties for each media package
    final Stream<Property> props = $(mps).bind(new Fn<String, Stream<Property>>() {
      @Override public Stream<Property> apply(final String mp) {
        // create a random amount of random properties
        return Stream.cont(inc()).take(random.nextInt(params.maxProps - params.minProps + 1) + params.minProps).map(new Fn<Integer, Property>() {
          @Override public Property apply(Integer ignore) {
            // try to pick a free property a 100 times
            for (int i = 0; i < 100; i++) {
              // randomly select a property name
              final PropertyName pName = propertyNames[random.nextInt(propertyNames.length)];
              // check if the selected property is already associated with the current media package
              final ASelectQuery doesPropertyExist = q.select(q.properties(pName)).where(q.mediaPackageId(mp));
              if (sizeOf(doesPropertyExist.run().getRecords().bind(ARecords.getProperties)) == 0) {
                // create a property with a randomly picked value
                final Property p = Property.mk(PropertyId.mk(mp, pName), params.values[random.nextInt(params.values.length)]);
                if (am.setProperty(p))
                  return p;
              }
            }
            fail("Cannot pick another random property that has not been inserted yet");
            return null;
          }
        });
      }
    }).eval(); // evaluate stream to fill the database, otherwise unexpected results will occur due to stream laziness
    assertThat("Number of generated properties",
               sizeOf(props), allOf(greaterThanOrEqualTo(params.mpCount * params.minProps),
                                    lessThanOrEqualTo(params.mpCount * params.maxProps)));
    // iterate all properties and try to retrieve them from the AssetManager
    for (final Property prop : props) {
      final AResult r = q.select(params.mkTarget.apply(prop))
              .where(params.mkWhere.apply(prop))
              .run();
      // get all properties of the result records
      assertThat("Number of records", r.getSize(), params.expectRecords);
      final Stream<Property> allProps = r.getRecords().bind(ARecords.getProperties);
      assertThat("Total number of properties: " + allProps.mkString(", "), sizeOf(allProps), params.expectPropertiesTotal);
      assertThat("Total number of snapshots", sizeOf(r.getRecords().bind(ARecords.getSnapshot)), params.expectSnapshotsTotal);
      final Stream<Property> findSavedProperty = r.getRecords().bind(ARecords.getProperties).filter(Booleans.eq(prop));
      if (params.expectContainsSavedProperty) {
        assertThat("Contains saved property", findSavedProperty, hasItem(prop));
      }
    }
  }

  private Object parametersForTestPropertyRetrieval() throws Exception {
    setUp(mkAbstractAssetManager());
    return $a(
            // Fetch one property of the latest version of a media package.
            new Params()
                    .propertyNameSetSize(20000)
                    .generateProperties(1, 10)
                    .mkTarget(new Fn<Property, Target[]>() {
                      @Override public Target[] apply(Property p) {
                        return $a(q.properties(p.getId().getFqn()));
                      }
                    })
                    .mkWhere(new Fn<Property, Predicate>() {
                      @Override public Predicate apply(Property p) {
                        return q.mediaPackageId(p.getId().getMediaPackageId()).and(q.version().isLatest());
                      }
                    })
                    .expectRecords(equalTo(1L))
                    .expectPropertiesTotal(equalTo(1))
                    .expectSnapshotsTotal(equalTo(0))
                    .expectContainsSavedProperty(true),
            //
            // The difference between this fixture and the above is only an additional property predicate which
            //   will not influence the result set.
            new Params()
                    .propertyNameSetSize(10)
                    .generateProperties(5, 5)
                    .mkTarget(new Fn<Property, Target[]>() {
                      @Override public Target[] apply(Property p) {
                        return $a(q.properties(p.getId().getFqn()));
                      }
                    })
                    .mkWhere(new Fn<Property, Predicate>() {
                      @Override public Predicate apply(Property p) {
                        return q.mediaPackageId(p.getId().getMediaPackageId())
                                .and(q.version().isLatest())
                                .and(q.property(Value.UNTYPED, p.getId().getFqn()).exists());
                      }
                    })
                    .expectRecords(equalTo(1L))
                    .expectPropertiesTotal(equalTo(1))
                    .expectSnapshotsTotal(equalTo(0))
                    .expectContainsSavedProperty(true),
            //
            // Fetch all properties of the matched records.
            new Params()
                    .propertyNameSetSize(10)
                    .generateProperties(5, 5)
                    .mkTarget(new Fn<Property, Target[]>() {
                      @Override public Target[] apply(Property p) {
                        return $a(q.properties());
                      }
                    })
                    .mkWhere(new Fn<Property, Predicate>() {
                      @Override public Predicate apply(Property p) {
                        final ValueType<Object> t = (ValueType<Object>) p.getValue().getType();
                        return q.mediaPackageId(p.getId().getMediaPackageId())
                                .and(q.version().isLatest())
                                .and(q.property(t, p.getId().getFqn()).eq(Values.getValueUntyped(p.getValue())));
                      }
                    })
                    .expectRecords(equalTo(1L))
                    .expectPropertiesTotal(equalTo(5))
                    .expectSnapshotsTotal(equalTo(0))
                    .expectContainsSavedProperty(true),
            //
            new Params()
                    .propertyNameSetSize(20)
                    .generateProperties(5, 5)
                    .mkTarget(new Fn<Property, Target[]>() {
                      @Override public Target[] apply(Property p) {
                        return $a(q.snapshot());
                      }
                    })
                    .mkWhere(new Fn<Property, Predicate>() {
                      @Override public Predicate apply(Property p) {
                        final ValueType<Object> t = (ValueType<Object>) p.getValue().getType();
                        return q.mediaPackageId(p.getId().getMediaPackageId())
                                .and(q.version().isLatest())
                                .and(q.property(t, p.getId().getFqn()).eq(Values.getValueUntyped(p.getValue())));
                      }
                    })
                    .expectRecords(equalTo(1L))
                    .expectPropertiesTotal(equalTo(0))
                    .expectSnapshotsTotal(equalTo(1))
                    .expectContainsSavedProperty(false),
            //
            // Fetch two times the same property of the latest version of a media package.
            new Params()
                    .propertyNameSetSize(15)
                    .generateProperties(1, 10)
                    .mkTarget(new Fn<Property, Target[]>() {
                      @Override public Target[] apply(Property p) {
                        return $a(q.properties(p.getId().getFqn()), q.properties(p.getId().getFqn()));
                      }
                    })
                    .mkWhere(new Fn<Property, Predicate>() {
                      @Override public Predicate apply(Property p) {
                        return q.mediaPackageId(p.getId().getMediaPackageId()).and(q.version().isLatest());
                      }
                    })
                    .expectRecords(equalTo(1L))
                    .expectPropertiesTotal(equalTo(1))
                    .expectSnapshotsTotal(equalTo(0))
                    .expectContainsSavedProperty(true),
            //
            new Params()
                    .propertyNameSetSize(50)
                    .generateProperties(1, 10)
                    .mkTarget(new Fn<Property, Target[]>() {
                      @Override public Target[] apply(Property p) {
                        return $a(q.properties(p.getId().getFqn()));
                      }
                    })
                    .mkWhere(new Fn<Property, Predicate>() {
                      @Override public Predicate apply(Property p) {
                        return q.hasPropertiesOf(p.getId().getNamespace());
                      }
                    })
                    .expectRecords(greaterThanOrEqualTo(1L))
                    .expectPropertiesTotal(greaterThanOrEqualTo(1))
                    .expectSnapshotsTotal(equalTo(0))
                    .expectContainsSavedProperty(true)
    );
  }

  private static String randomString() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private static PropertyName[] createRandomPropertyNames(int number) {
    final PropertyName[] ps = new PropertyName[number];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = PropertyName.mk("ns-" + randomString(), "p-" + randomString());
    }
    return ps;
  }

  /**
   * Parameter holder.
   */
  class Params {
    // CHECKSTYLE:OFF
    int mpCount = 50;
    Value[] values = $a(Value.mk(true), Value.mk(new Date()), Value.mk("a"), Value.mk(1L), Value.mk("b"));
    int propertyNameSetSize = 100;
    int minProps = 1;
    int maxProps = 10;
    Fn<Property, Target[]> mkTarget = new Fn<Property, Target[]>() {
      @Override public Target[] apply(Property property) {
        return new Target[0];
      }
    };
    Fn<Property, Predicate> mkWhere = new Fn<Property, Predicate>() {
      @Override public Predicate apply(Property property) {
        return q.always();
      }
    };
    Matcher<Long> expectRecords = anyOf();
    Matcher<Integer> expectPropertiesTotal = anyOf();
    Matcher<Integer> expectSnapshotsTotal = anyOf();
    boolean expectContainsSavedProperty = true;
    // CHECKSTYLE:ON

    /** Amount of media packages to create. */
    Params mpCount(int a) {
      this.mpCount = a;
      return this;
    }

    /** Set of values to select from randomly. */
    Params values(Value[] a) {
      this.values = a;
      return this;
    }

    /** Number of randomly created property names to select from. */
    Params propertyNameSetSize(int a) {
      this.propertyNameSetSize = a;
      return this;
    }

    /** Minimum and maximum amount of random properties per media package. */
    Params generateProperties(int min, int max) {
      this.minProps = min;
      this.maxProps = max;
      return this;
    }

    /** Create select target. */
    Params mkTarget(Fn<Property, Target[]> a) {
      this.mkTarget = a;
      return this;
    }

    /** Create where predicate. */
    Params mkWhere(Fn<Property, Predicate> a) {
      this.mkWhere = a;
      return this;
    }

    /** Number of expected records. */
    Params expectRecords(Matcher<Long> a) {
      this.expectRecords = a;
      return this;
    }

    /** Total number of expected properties (properties per record times number of records). */
    Params expectPropertiesTotal(Matcher<Integer> a) {
      this.expectPropertiesTotal = a;
      return this;
    }

    /** Total number of expected snapshots (snapshot per record times number of records). */
    Params expectSnapshotsTotal(Matcher<Integer> a) {
      this.expectSnapshotsTotal = a;
      return this;
    }

    /** Set to true if the property passed to the target and where construction functions is expected in the total set of properties. */
    Params expectContainsSavedProperty(boolean a) {
      this.expectContainsSavedProperty = a;
      return this;
    }
  }
}
