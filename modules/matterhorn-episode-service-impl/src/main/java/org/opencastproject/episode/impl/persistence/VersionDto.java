/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.episode.impl.persistence;

import org.opencastproject.util.data.Option;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.PersistenceUtil.runSingleResultQuery;

@Entity(name = "Version")
@Table(name = "episode_version")
@NamedQueries({
        @NamedQuery(name = "Version.last", query = "select a from Version a where a.mediaPackageId = :mediaPackageId") })
public final class VersionDto {
  @Id
  @Column(name = "mediapackageid")
  private String mediaPackageId;

  @Column(name = "last_version", nullable = false)
  private long lastVersion;

  public static VersionDto create(String mediaPackageId, long lastVersion) {
    final VersionDto dto = new VersionDto();
    dto.mediaPackageId = mediaPackageId;
    dto.lastVersion = lastVersion;
    return dto;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public long getLastVersion() {
    return lastVersion;
  }

  public static Option<VersionDto> findLast(EntityManager em, String mediaPackageId) {
    return runSingleResultQuery(em, "Version.last", tuple("mediaPackageId", mediaPackageId));
  }
}
