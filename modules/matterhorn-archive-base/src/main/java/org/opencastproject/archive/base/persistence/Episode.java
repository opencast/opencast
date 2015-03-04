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
package org.opencastproject.archive.base.persistence;

import org.opencastproject.archive.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlList;

import java.util.Date;

/** An archived media package. */
public final class Episode {
  private final Version version;
  private final String organization;
  private final boolean deleted;
  private final Date modificationDate;
  private final AccessControlList acl;
  private final MediaPackage mediaPackage;

  public Episode(MediaPackage mediaPackage, Version version, String organization, AccessControlList acl,
          Date modificationDate, boolean deleted) {
    this.version = version;
    this.organization = organization;
    this.deleted = deleted;
    this.modificationDate = modificationDate;
    this.acl = acl;
    this.mediaPackage = mediaPackage;
  }

  public Version getVersion() {
    return version;
  }

  public String getOrganization() {
    return organization;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public Date getModificationDate() {
    return modificationDate;
  }

  public AccessControlList getAcl() {
    return acl;
  }

  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }
}
