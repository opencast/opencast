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

package org.opencastproject.serviceregistry.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencastproject.db.DBTestEnv.newDBSession;

import org.opencastproject.db.DBSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DispatchLeaseManagerTest {

  private DBSession db;

  @BeforeEach
  public void setUp() {
    db = newDBSession("org.opencastproject.common");
  }

  @AfterEach
  public void tearDown() {
    db.close();
  }

  @Test
  public void onlyOneOfMultipleCandidatesAcquiresTheLease() {
    DispatchLeaseManager nodeA = new DispatchLeaseManager(db, "nodeA", 60);
    DispatchLeaseManager nodeB = new DispatchLeaseManager(db, "nodeB", 60);
    DispatchLeaseManager nodeC = new DispatchLeaseManager(db, "nodeC", 60);

    assertTrue(nodeA.tryAcquireOrRenew());
    assertFalse(nodeB.tryAcquireOrRenew());
    assertFalse(nodeC.tryAcquireOrRenew());
  }

  @Test
  public void leaderCanRenewItsOwnLeaseRepeatedly() {
    DispatchLeaseManager leader = new DispatchLeaseManager(db, "leader", 60);
    DispatchLeaseManager other = new DispatchLeaseManager(db, "other", 60);

    assertTrue(leader.tryAcquireOrRenew());
    assertTrue(leader.tryAcquireOrRenew());
    assertTrue(leader.tryAcquireOrRenew());
    assertFalse(other.tryAcquireOrRenew());
  }

  @Test
  public void anotherNodeCanAcquireAfterLeaseExpires() throws InterruptedException {
    DispatchLeaseManager leader = new DispatchLeaseManager(db, "leader", 1);
    DispatchLeaseManager successor = new DispatchLeaseManager(db, "successor", 60);

    assertTrue(leader.tryAcquireOrRenew());
    assertFalse(successor.tryAcquireOrRenew());

    Thread.sleep(1500);

    assertTrue(successor.tryAcquireOrRenew());
    assertFalse(leader.tryAcquireOrRenew());
  }

  @Test
  public void releaseAllowsImmediateTakeoverWithoutWaitingForTtl() {
    DispatchLeaseManager leader = new DispatchLeaseManager(db, "leader", 60);
    DispatchLeaseManager successor = new DispatchLeaseManager(db, "successor", 60);

    assertTrue(leader.tryAcquireOrRenew());
    assertFalse(successor.tryAcquireOrRenew());

    leader.release();

    assertTrue(successor.tryAcquireOrRenew());
  }

  @Test
  public void releaseIsANoOpWhenNotHoldingTheLease() {
    DispatchLeaseManager leader = new DispatchLeaseManager(db, "leader", 60);
    DispatchLeaseManager other = new DispatchLeaseManager(db, "other", 60);

    assertTrue(leader.tryAcquireOrRenew());

    other.release();

    assertFalse(other.tryAcquireOrRenew());
  }
}
