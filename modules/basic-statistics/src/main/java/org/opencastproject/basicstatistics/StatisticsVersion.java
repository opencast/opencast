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
package org.opencastproject.basicstatistics;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Records when each version of the statistics system became active. A new row is only ever appended when the
 * running code's version differs from the most recently recorded one — existing rows are never modified. A version
 * can therefore be recorded more than once, e.g. after downgrading and upgrading again.
 *
 * The rows form a numbered chain: each row has a revision, one higher than its predecessor. The revision is unique,
 * so if several nodes start at the same time and see the same history, the database lets only one of them append the
 * next row. This avoids duplicate rows that are caused by nothing but timing.
 *
 * This lets callers determine a "data may be incomplete before this date" threshold: by default, the timestamp
 * of the version-1 row, i.e. when statistics tracking first existed at all.
 */
@Entity(name = "StatisticsVersion")
@Table(name = "oc_basic_statistics_version", uniqueConstraints = @UniqueConstraint(columnNames = "revision"))
@NamedQueries({
    @NamedQuery(
        name = "StatisticsVersion.findLatest",
        query = "SELECT v FROM StatisticsVersion v ORDER BY v.revision DESC"
    ),
    @NamedQuery(
        name = "StatisticsVersion.findByVersion",
        query = "SELECT v FROM StatisticsVersion v WHERE v.version = :version "
            + "ORDER BY v.activatedAt ASC, v.revision ASC"
    ),
})
public class StatisticsVersion {

  /** Bump this whenever a change affects whether historical statistics data can be considered complete. */
  public static final int CURRENT_VERSION = 1;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  private long id;

  /** Position of this row in the history, starting at 1. Unique, see the class documentation. */
  @Column(name = "revision", nullable = false)
  private long revision;

  /** Named "stat_version" rather than "version" since the latter could be a reserved word. */
  @Column(name = "stat_version", nullable = false)
  private int version;

  @Column(name = "activated_at", nullable = false)
  private Instant activatedAt;

  public StatisticsVersion() {
  }

  public StatisticsVersion(long revision, int version, Instant activatedAt) {
    this.revision = revision;
    this.version = version;
    this.activatedAt = activatedAt;
  }

  public long getId() {
    return id;
  }

  public long getRevision() {
    return revision;
  }

  public int getVersion() {
    return version;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }
}
