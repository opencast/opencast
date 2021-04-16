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
package org.opencastproject.assetmanager.impl.persistence;

import org.opencastproject.util.persistencefn.Queries;

import com.entwinemedia.fn.ProductBuilder;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.data.Opt;

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
        name = "VersionClaim.update",
        query = "update VersionClaim a set a.lastClaimed = :lastClaimed where a.mediaPackageId = :mediaPackageId"
    )
})
public final class VersionClaimDto {
  private static final ProductBuilder p = Products.E;

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
  public static Opt<VersionClaimDto> findLast(EntityManager em, String mediaPackageId) {
    return Queries.named.findSingle(em, "VersionClaim.last", p.p2("mediaPackageId", mediaPackageId));
  }

  /** Update the last claimed version of a media package. */
  public static boolean update(EntityManager em, String mediaPackageId, long lastClaimed) {
    return Queries.named.update(
            em, "VersionClaim.update",
            p.p2("mediaPackageId", mediaPackageId),
            p.p2("lastClaimed", lastClaimed));
  }
}
