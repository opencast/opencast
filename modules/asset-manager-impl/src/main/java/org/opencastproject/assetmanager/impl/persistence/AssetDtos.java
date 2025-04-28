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

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.impl.VersionImpl;

/**
 * Collection of non-JPA DTOs to tuple up the {@link AssetDto} JPA entity with some additional joined data.
 */
public final class AssetDtos {
  private AssetDtos() {
  }

  /**
   * With some more data.
   */
  public static class Medium {
    private final AssetDto assetDto;
    private final String availability;
    private final String storageId;
    private final String organizationId;
    private final String owner;

    public Medium(AssetDto assetDto, Availability availability, String organizationId) {
      this.assetDto = assetDto;
      this.availability = availability.name();
      this.storageId = null;
      this.organizationId = organizationId;
      this.owner = null;
    }

    public Medium(AssetDto assetDto, Availability availability, String storageId, String organizationId, String owner) {
      this.assetDto = assetDto;
      this.availability = availability.name();
      this.storageId = storageId;
      this.organizationId = organizationId;
      this.owner = owner;
    }

    public AssetDto getAssetDto() {
      return assetDto;
    }

    public Availability getAvailability() {
      return Availability.valueOf(availability);
    }

    public String getStorageId() {
      return storageId;
    }

    public String getOrganizationId() {
      return organizationId;
    }

    public String getOwner() {
      return owner;
    }
  }

  /**
   * ... and even more.
   */
  public static class Full extends Medium {
    private final String mediaPackageId;
    private final VersionImpl version;

    public Full(AssetDto assetDto, Availability availability, String storageId, String organizationId, String owner,
        String mediaPackageId, VersionImpl version) {
      super(assetDto, availability, storageId, organizationId, owner);
      this.mediaPackageId = mediaPackageId;
      this.version = version;
    }

    public String getMediaPackageId() {
      return mediaPackageId;
    }

    public VersionImpl getVersion() {
      return version;
    }
  }
}
