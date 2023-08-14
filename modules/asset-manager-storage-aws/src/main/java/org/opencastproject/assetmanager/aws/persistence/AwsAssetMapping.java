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

package org.opencastproject.assetmanager.aws.persistence;

import java.util.Date;

public final class AwsAssetMapping {
  private final String organizationId;
  private final String mediaPackageId;
  private final String mediaPackageElementId;
  private final Long version;
  // This is the AWS object key
  private final String objectKey;
  // This is the AWS object version (DIFFERENT FROM MH version, this is used for recovery when the object is deleted)
  private final String objectVersion;
  // Date/time this asset was deleted
  private final Date deletionDate;

  public AwsAssetMapping(String organizationId, String mediaPackageId, String mediaPackageElementId, Long version,
          String objectKey, String objectVersion, Date deletedDate) {
    super();
    this.organizationId = organizationId;
    this.mediaPackageId = mediaPackageId;
    this.mediaPackageElementId = mediaPackageElementId;
    this.version = version;
    this.objectKey = objectKey;
    this.objectVersion = objectVersion;
    this.deletionDate = deletedDate;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getMediaPackageElementId() {
    return mediaPackageElementId;
  }

  public Long getVersion() {
    return version;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getObjectVersion() {
    return objectVersion;
  }

  public Date getDeletionDate() {
    return deletionDate;
  }
}
