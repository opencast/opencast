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

package org.opencastproject.serviceregistry.impl.jpa;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A single-row lease used to elect exactly one active job dispatcher across a cluster of candidate nodes. There is
 * always exactly one row, with a fixed id, representing the current lease holder and when that lease expires.
 */
@Entity(name = "DispatchLock")
@Access(AccessType.FIELD)
@Table(name = "oc_dispatch_lock")
@NamedQueries({
    @NamedQuery(
        name = "DispatchLock.acquire",
        query = "UPDATE DispatchLock d SET d.owner = :owner, d.leaseExpires = :leaseExpires "
            + "WHERE d.id = :id AND (d.owner = :owner OR d.leaseExpires < CURRENT_TIMESTAMP)"
    ),
    @NamedQuery(
        name = "DispatchLock.release",
        query = "UPDATE DispatchLock d SET d.owner = null, d.leaseExpires = CURRENT_TIMESTAMP "
            + "WHERE d.id = :id AND d.owner = :owner"
    ),
})
public class DispatchLockJpaImpl {

  /** The fixed id of the single lock row used cluster-wide. */
  public static final long SINGLETON_ID = 1L;

  @Id
  @Column(name = "id")
  private Long id;

  @Column(name = "owner")
  private String owner;

  /**
   * Defaults to the epoch (i.e. already expired) rather than null: a SQL comparison against a null timestamp never
   * evaluates to true, which would otherwise make a freshly seeded row impossible for anyone to ever acquire.
   */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "lease_expires", nullable = false)
  private Date leaseExpires = new Date(0);

  public DispatchLockJpaImpl() {
  }

  public DispatchLockJpaImpl(long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Date getLeaseExpires() {
    return leaseExpires;
  }

  public void setLeaseExpires(Date leaseExpires) {
    this.leaseExpires = leaseExpires;
  }
}
