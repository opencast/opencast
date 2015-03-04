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
package org.opencastproject.archive.base.storage;

import org.opencastproject.archive.api.Version;
import org.opencastproject.util.data.Option;

import static org.opencastproject.util.EqualsUtil.eqObj;
import static org.opencastproject.util.EqualsUtil.hash;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

public final class DeletionSelector {
  private final String mediaPackageId;
  private final String organizationId;
  private final Option<Version> version;

  public DeletionSelector(String organizationId, String mediaPackageId, Option<Version> version) {
    this.mediaPackageId = mediaPackageId;
    this.organizationId = organizationId;
    this.version = version;
  }

  public static DeletionSelector del(String organizationId, String mediaPackageId, Version version) {
    return new DeletionSelector(organizationId, mediaPackageId, some(version));
  }

  public static DeletionSelector delAll(String organizationId, String mediaPackageId) {
    return new DeletionSelector(organizationId, mediaPackageId, none(Version.class));
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public Option<Version> getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof DeletionSelector && eqFields((DeletionSelector) that));
  }

  private boolean eqFields(DeletionSelector that) {
    return eqObj(this.mediaPackageId, that.mediaPackageId)
            && eqObj(this.organizationId, that.organizationId)
            && eqObj(this.version, that.version);
  }

  @Override
  public int hashCode() {
    return hash(mediaPackageId, organizationId, version);
  }
}
