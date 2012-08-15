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
package org.opencastproject.episode.impl;

import org.opencastproject.episode.api.Version;
import org.opencastproject.util.EqualsUtil;

import static org.opencastproject.util.EqualsUtil.eqObj;

/** A vector that uniquely identifies a stored media package element. */
public final class StoragePath {
  private final String mediaPackageId;
  private final String mediaPackageElementId;
  private final String organizationId;
  private final Version version;

  public StoragePath(String organizationId, String mediaPackageId, Version version, String mediaPackageElementId) {
    this.mediaPackageId = mediaPackageId;
    this.mediaPackageElementId = mediaPackageElementId;
    this.organizationId = organizationId;
    this.version = version;
  }

  public static StoragePath spath(String organizationId, String mediaPackageId, Version version,
                                  String mediaPackageElementId) {
    return new StoragePath(organizationId, mediaPackageId, version, mediaPackageElementId);
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getMediaPackageElementId() {
    return mediaPackageElementId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public Version getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof StoragePath && eqFields((StoragePath) that));
  }

  private boolean eqFields(StoragePath that) {
    return eqObj(this.mediaPackageId, that.mediaPackageId)
            && eqObj(this.mediaPackageElementId, that.mediaPackageElementId)
            && eqObj(this.organizationId, that.organizationId)
            && eqObj(this.version, that.version);
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(mediaPackageId, mediaPackageElementId, organizationId, version);
  }

  @Override public String toString() {
    return "[StoragePath orgId=" + organizationId
            + " mpId=" + mediaPackageId
            + " version=" + version
            + " mpeId=" + mediaPackageElementId
            + "]";
  }
}
