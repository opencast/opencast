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
package org.opencastproject.assetmanager.impl.storage;

import static com.entwinemedia.fn.Equality.eq;

import org.opencastproject.assetmanager.api.Version;

import com.entwinemedia.fn.Equality;

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * A vector that uniquely identifies a stored media package asset.
 */
@ParametersAreNonnullByDefault
@Immutable
public final class StoragePath implements Serializable {
  private static final long serialVersionUID = -5646543990835098350L;

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

  public static StoragePath mk(
          String organizationId, String mediaPackageId, Version version, String mediaPackageElementId) {
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
    return eq(this.mediaPackageId, that.mediaPackageId)
            && eq(this.mediaPackageElementId, that.mediaPackageElementId)
            && eq(this.organizationId, that.organizationId)
            && eq(this.version, that.version);
  }

  @Override
  public int hashCode() {
    return Equality.hash(mediaPackageId, mediaPackageElementId, organizationId, version);
  }

  @Override public String toString() {
    return "StoragePath(orgId=" + organizationId
            + ", mpId=" + mediaPackageId
            + ", version=" + version
            + ", mpeId=" + mediaPackageElementId
            + ")";
  }
}
