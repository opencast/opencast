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
package org.opencastproject.assetmanager.api;

import org.opencastproject.mediapackage.MediaPackage;

import java.util.Date;

/**
 * A versioned snapshot of a {@link MediaPackage} under the control of the {@link AssetManager}.
 */
public interface Snapshot {
  /** Get the version. */
  Version getVersion();

  /** Get the ID of the organization where this media package belongs to. */
  String getOrganizationId();

  /** Tell about when this version of the episode has been stored in the AssetManager. */
  Date getArchivalDate();

  /** Get the availability of the media package's assets. */
  Availability getAvailability();

  /** Get the store ID of the asset store where this snapshot currently lives */
  String getStorageId();

  /** Get the owner of the snapshot. **/
  String getOwner();

  /**
   * Get the media package.
   * <p>
   * Implementations are required to provide media package element URIs that point to some valid HTTP endpoint.
   */
  MediaPackage getMediaPackage();
}
