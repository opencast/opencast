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

import static org.opencastproject.db.Queries.namedQuery;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Supports the determination of the next free version identifier.
 */
@Entity(name = "VersionClaim")
@Table(name = "oc_assets_version_claim")
@NamedQueries({
    @NamedQuery(
        name = "VersionClaim.last",
        query = "select a from VersionClaim a where a.mediaPackageId = :mediaPackageId"
    ),
    @NamedQuery(
        name = "VersionClaim.increment",
        query = "update VersionClaim a set a.lastClaimed = a.lastClaimed + 1 where a.mediaPackageId = :mediaPackageId"
    )
})
public class VersionClaimDto {
  @Id
  @Column(name = "mediapackage_id", length = 128)
  private String mediaPackageId;

  @Column(name = "last_claimed", nullable = false)
  private long lastClaimed;

  /** Create a new DTO. */
  public static VersionClaimDto mk(String mediaPackageId, long lastClaimed) {
    final VersionClaimDto dto = new VersionClaimDto();
    dto.mediaPackageId = mediaPackageId;
    dto.lastClaimed = lastClaimed;
    return dto;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public long getLastClaimed() {
    return lastClaimed;
  }

  /** Find the last claimed version for a media package. */
  public static Function<EntityManager, Optional<VersionClaimDto>> findLastQuery(String mediaPackageId) {
    return namedQuery.findOpt(
        "VersionClaim.last",
        VersionClaimDto.class,
        Pair.of("mediaPackageId", mediaPackageId)
    );
  }

  /**
   * Atomically increment the last claimed version of a media package by one.
   *
   * @return the number of rows updated: 1 if a row for this media package already existed, 0 if not.
   */
  public static Function<EntityManager, Integer> incrementQuery(String mediaPackageId) {
    return namedQuery.update(
        "VersionClaim.increment",
        Pair.of("mediaPackageId", mediaPackageId)
    );
  }
}
