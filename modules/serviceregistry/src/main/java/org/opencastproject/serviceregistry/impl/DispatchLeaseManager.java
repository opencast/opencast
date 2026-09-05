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

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.serviceregistry.impl.jpa.DispatchLockJpaImpl;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Elects exactly one job dispatcher across a cluster of candidate nodes by acquiring and renewing a lease stored in
 * a single, shared database row. Only the node currently holding the lease should perform job dispatching; all
 * other candidate nodes must sit idle until they acquire it, e.g. after the current holder stops renewing it.
 *
 * <p>Acquisition and renewal are both done with a single, portable JPQL bulk update whose {@code WHERE} clause
 * compares the stored expiry against the database's own {@code CURRENT_TIMESTAMP}, not the calling node's local
 * clock. This makes the race between competing nodes safe by relying purely on the database engine's row-level
 * locking during the update, without needing vendor-specific advisory locks, and makes every competing node judge
 * lease freshness against one shared clock rather than against each other's local clocks.
 */
public class DispatchLeaseManager {

  private static final Logger logger = LoggerFactory.getLogger(DispatchLeaseManager.class);

  private final DBSession db;
  private final String owner;
  private final long leaseTimeoutSeconds;

  public DispatchLeaseManager(DBSession db, String owner, long leaseTimeoutSeconds) {
    this.db = db;
    this.owner = owner;
    this.leaseTimeoutSeconds = leaseTimeoutSeconds;
  }

  /**
   * Attempt to acquire the dispatch lease, or renew it if this node already holds it.
   *
   * @return true if this node is (now, or still) the elected dispatcher, false otherwise
   */
  public boolean tryAcquireOrRenew() {
    ensureSeeded();

    Date newExpiry = new Date(System.currentTimeMillis() + leaseTimeoutSeconds * 1000);
    int updated = db.execTx(namedQuery.update(
        "DispatchLock.acquire",
        Pair.of("id", DispatchLockJpaImpl.SINGLETON_ID),
        Pair.of("owner", owner),
        Pair.of("leaseExpires", newExpiry)
    ));

    boolean isLeader = updated == 1;
    logger.trace("Dispatch lease acquire/renew attempt by '{}': {}", owner, isLeader ? "leader" : "not leader");
    return isLeader;
  }

  /**
   * Release the lease if this node currently holds it, allowing another candidate node to take over immediately
   * instead of waiting for the lease to expire. Safe to call even if this node isn't the current leader.
   */
  public void release() {
    int updated = db.execTx(namedQuery.update(
        "DispatchLock.release",
        Pair.of("id", DispatchLockJpaImpl.SINGLETON_ID),
        Pair.of("owner", owner)
    ));
    if (updated == 1) {
      logger.debug("Released dispatch lease held by '{}'", owner);
    }
  }

  /**
   * Make sure the singleton lock row exists. Safe to call from multiple nodes concurrently; only one insert wins,
   * the rest are simply ignored.
   */
  private void ensureSeeded() {
    boolean exists = db.exec(namedQuery.findByIdOpt(DispatchLockJpaImpl.class, DispatchLockJpaImpl.SINGLETON_ID))
        .isPresent();
    if (exists) {
      return;
    }
    try {
      db.execTx(namedQuery.persist(new DispatchLockJpaImpl(DispatchLockJpaImpl.SINGLETON_ID)));
      logger.debug("Seeded dispatch lease row");
    } catch (Exception e) {
      // Another node already seeded the row concurrently. That's fine, we can proceed.
      logger.trace("Dispatch lease row already seeded by another node", e);
    }
  }
}
