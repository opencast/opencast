/*
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

package org.opencastproject.assetmanager.impl.persistence;

import static org.junit.Assert.assertEquals;

import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.db.DBTestEnv;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseTest {

  private static final String PERSISTENCE_UNIT = "org.opencastproject.assetmanager.impl";

  /**
   * Many concurrent claims for the same, brand new media package must each receive a distinct version: the
   * underlying UPDATE is atomic at the database level, so no amount of thread interleaving should produce a
   * duplicate. See https://github.com/opencast/opencast/issues/7918.
   */
  @Test
  public void claimVersionUnderConcurrencyNeverDuplicates() throws Exception {
    final Database database = new Database(DBTestEnv.newDBSession(PERSISTENCE_UNIT));
    final String mpId = UUID.randomUUID().toString();
    final int concurrentClaims = 25;

    final ExecutorService executor = Executors.newFixedThreadPool(concurrentClaims);
    final CyclicBarrier barrier = new CyclicBarrier(concurrentClaims);
    final List<Callable<Long>> tasks = new ArrayList<>();
    for (int i = 0; i < concurrentClaims; i++) {
      tasks.add(() -> {
        barrier.await();
        return database.claimVersion(mpId).value();
      });
    }

    final List<Future<Long>> futures = executor.invokeAll(tasks);
    executor.shutdown();

    final Set<Long> claimedVersions = new HashSet<>();
    for (Future<Long> future : futures) {
      claimedVersions.add(future.get());
    }

    assertEquals("every concurrent claim must receive a distinct version, with none lost to a race",
            concurrentClaims, claimedVersions.size());
  }

  @Test
  public void claimVersionStartsAtFirstAndIncrementsSequentially() throws Exception {
    final Database database = new Database(DBTestEnv.newDBSession(PERSISTENCE_UNIT));
    final String mpId = UUID.randomUUID().toString();

    assertEquals(VersionImpl.FIRST.value(), database.claimVersion(mpId).value());
    assertEquals(VersionImpl.FIRST.value() + 1, database.claimVersion(mpId).value());
    assertEquals(VersionImpl.FIRST.value() + 2, database.claimVersion(mpId).value());
  }
}
