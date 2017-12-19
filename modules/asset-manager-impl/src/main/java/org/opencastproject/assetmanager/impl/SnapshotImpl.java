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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.mediapackage.MediaPackage;

import java.util.Date;

public class SnapshotImpl implements Snapshot {
  private final Version version;
  private final String organizationId;
  private final Date archivalDate;
  private final Availability availability;
  private final String owner;
  private final MediaPackage mediaPackage;

  public SnapshotImpl(
          Version version,
          String organizationId,
          Date archivalDate,
          Availability availability,
          String owner,
          MediaPackage mediaPackage) {
    this.version = version;
    this.organizationId = organizationId;
    this.archivalDate = archivalDate;
    this.availability = availability;
    this.mediaPackage = mediaPackage;
    this.owner = owner;
  }

  @Override public Version getVersion() {
    return version;
  }

  @Override public String getOrganizationId() {
    return organizationId;
  }

  @Override public Date getArchivalDate() {
    return archivalDate;
  }

  @Override public Availability getAvailability() {
    return availability;
  }

  @Override public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  @Override public String getOwner() {
    return owner;
  }
}
