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

import static java.lang.String.format;

import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.security.api.DefaultOrganization;

import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.P1Lazy;
import com.entwinemedia.fn.P2;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.ImmutableIteratorArrayAdapter;
import com.entwinemedia.fn.data.Opt;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.UUID;

/**
 * Performance tests.
 */
@Ignore
public class AbstractAssetManagerPerformanceTest extends AbstractAssetManagerTestBase {
  private static final Logger logger = LoggerFactory.getLogger(AbstractAssetManagerPerformanceTest.class);
  private static final int MEDIAPACKAGES_COUNT = 800;
  private static final String[] RANDOM_STRINGS = new String[200000];

  static {
    for (int i = 0; i < RANDOM_STRINGS.length; i++) {
      RANDOM_STRINGS[i] = UUID.randomUUID().toString();
    }
  }

  @Test
  public void testInsertSelectDelete() {
    // add
    final Snapshot[] snapshots = benchmark("take snapshot", new P1Lazy<P2<Snapshot[], Integer>>() {
      @Override public P2<Snapshot[], Integer> get1() {
        final Snapshot[] snapshots = createAndAddMediaPackages(MEDIAPACKAGES_COUNT, 1, 1, Opt.some("series"));
        final Iterator<String> randomStrings = new ImmutableIteratorArrayAdapter<>(RANDOM_STRINGS);
        for (Snapshot snapshot : snapshots) {
          am.setProperty(p.agent.mk(snapshot.getMediaPackage().getIdentifier().toString(), randomStrings.next()));
        }
        for (Snapshot snapshot : snapshots) {
          am.setProperty(p2.legacyId.mk(snapshot.getMediaPackage().getIdentifier().toString(), randomStrings.next()));
        }
        return Products.E.p2(snapshots, snapshots.length);
      }
    });
    // select
    benchmark("select", new P1Lazy<P2<Unit, Integer>>() {
      @Override public P2<Unit, Integer> get1() {
        for (Snapshot snapshot : snapshots) {
          q.select(q.snapshot())
                  .where(q.mediaPackageId(snapshot.getMediaPackage().getIdentifier().toString())
                                 .and(q.version().isLatest())
                                 .and(p.agent.eq(randomElem(RANDOM_STRINGS)))
                                 .and(p2.hasPropertiesOfNamespace()))
                  .run();
        }
        return Products.E.p2(Unit.unit, snapshots.length);
      }
    });
    // delete properties
    benchmark("delete properties", new P1Lazy<P2<Unit, Integer>>() {
      @Override public P2<Unit, Integer> get1() {
        final Iterator<String> randomStrings = new ImmutableIteratorArrayAdapter<>(RANDOM_STRINGS);
        for (Snapshot snapshot : snapshots) {
          final String mpId = snapshot.getMediaPackage().getIdentifier().toString();
          q.delete(OWNER, p.allProperties())
                  .where(q.mediaPackageId(mpId).and(q.organizationId(DefaultOrganization.DEFAULT_ORGANIZATION_ID)))
                  .run();
//          q.delete(p.allProperties())
//                  .where(q.mediaPackageId(snapshot.getMediaPackage().getIdentifier().toString())
//                                 .and(q.version().isLatest())
//                                 .and(p.agent.eq(randomStrings.next()))
//                                 .and(p2.hasPropertiesOfNamespace()))
//                  .run();
        }
        return Products.E.p2(Unit.unit, snapshots.length);
      }
    });
    // q.delete(q.propertiesOf(namespace)).where(q.organizationId(orgId).and(q.mediaPackageId(mpId))).run();
    if (true) return;
    benchmark("delete snapshots", new P1Lazy<P2<Unit, Integer>>() {
      @Override public P2<Unit, Integer> get1() {
        final Iterator<String> randomStrings = new ImmutableIteratorArrayAdapter<>(RANDOM_STRINGS);
        for (Snapshot snapshot : snapshots) {
          q.delete(OWNER, q.snapshot())
                  .where(q.mediaPackageId(snapshot.getMediaPackage().getIdentifier().toString())
                                 .and(q.version().isLatest())
                                 .and(p.agent.eq(randomStrings.next()))
                                 .and(p2.hasPropertiesOfNamespace()))
                  .run();
        }
        return Products.E.p2(Unit.unit, snapshots.length);
      }
    });
  }

  private <A> A randomElem(A[] as) {
    return as[((int) Math.max(0, Math.random() * as.length - 1))];
  }

  /** Let <code>p</code> return a value and the number of performed operations. */
  private <A> A benchmark(String name, P1<P2<A, Integer>> p) {
    final long start = System.nanoTime();
    final P2<A, Integer> a = p.get1();
    printStats(name, start, a.get2());
    return a.get1();
  }

  private void printStats(String name, long start, int ops) {
    long end = System.nanoTime();
    double elapsedSeconds = (end - start) / 1000000000.0d;
    double opsPerSecond = ops / elapsedSeconds;
    logger.info(format("Benchmark %s: ops=%d runtime=%.1fs ops/s=%.2f", name, ops, elapsedSeconds, opsPerSecond));
  }
}
