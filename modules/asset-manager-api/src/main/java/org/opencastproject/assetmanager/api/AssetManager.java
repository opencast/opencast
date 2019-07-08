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

import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.mediapackage.MediaPackage;

import com.entwinemedia.fn.data.Opt;

import java.util.List;

/**
 * The AssetManager stores versioned {@linkplain Snapshot snapshots} of media packages.
 * <p>
 * It also supports the association of {@linkplain Property properties} to a history of snapshots which is called an episode.
 *
 * <h1>Terms</h1>
 * <h2>Snapshot</h2>
 * A snapshot saves a particular version of a media package. Snapshots are immutable and can only be deleted.
 *
 * <h2>Episode</h2>
 * An episode is the set of snapshots of a media package.
 *
 * <h2>Properties</h2>
 * Properties are associated with an episode and have a volatile character.
 * They support the quick and easy storage of meta data.
 * This removes the need for services to create their own persistence layer if they want to associate metadata with a media package.
 *
 * <h1>Notes</h1>
 * Media package IDs are considered to be unique throughout the whole system.
 * The organization ID is just a discriminator and not necessary to uniquely identify a media package.
 */
public interface AssetManager {
  String DEFAULT_OWNER = "default";

  /**
   * Get the media package from the lates snapshot.
   *
   * @param mediaPackageId
   * @return mediapackage
   */
  Opt<MediaPackage> getMediaPackage(String mediaPackageId);

  /**
   * Take a versioned snapshot of a media package.
   * <p>
   * Snapshot are tagged with string identifying the owner. Only the owner
   * of a snapshot is allowed to delete it.
   * Ownership only affects the deletion of a snapshot.
   *
   * @param owner
   *          the owner of the snapshot, e.g. the name of the calling service
   */
  Snapshot takeSnapshot(String owner, MediaPackage mp);

  /**
   * Take a versioned snapshot of a media package using the owner of the last snapshot or the default owner if it
   * does not exist.
   *
   * @param mediaPackage
   *          The media package to snapshot
   * @return A new snapshot
   */
  Snapshot takeSnapshot(MediaPackage mediaPackage);

  /**
   * Get the asset that is uniquely identified by the triple {version, media package ID, media package element ID}.
   *
   * @param version the version
   * @param mpId the media package ID
   * @param mpeId the media package element ID
   * @return the asset or none, if no such asset exists
   */
  Opt<Asset> getAsset(Version version, String mpId, String mpeId);

  /**
   * Set the state of availability of the assets of a snapshot.
   */
  void setAvailability(Version version, String mpId, Availability availability);

  // Properties

  /**
   * Set a property. Use this method to either insert a new property or update an existing one.
   * Properties are stored per episode.
   *
   * @return false, if the referenced episode does not exist.
   */
  boolean setProperty(Property property);

  /**
   * Delete all properties for a given media package identifier
   *
   * @param mediaPackageId
   *          Media package identifier
   * @return Number of deleted properties
   */
  int deleteProperties(String mediaPackageId);

  /**
   * Delete all properties for a given media package identifier and namespace.
   *
   * @param mediaPackageId
   *          Media package identifier
   * @param namespace
   *          A namespace prefix to use for deletion
   * @return Number of deleted properties
   */
  int deleteProperties(String mediaPackageId, String namespace);

  /**
   * Check if any snapshot with the given media package identifier exists.
   *
   * @param mediaPackageId
   *          The media package identifier to check for
   * @return If a snapshot exists for the given media package
   */
  boolean snapshotExists(String mediaPackageId);

  /**
   * Check if any snapshot with the given media package identifier exists.
   *
   * @param mediaPackageId
   *          The media package identifier to check for
   * @param organization
   *          The organization to limit the search to
   * @return If a snapshot exists for the given media package
   */
  boolean snapshotExists(String mediaPackageId, String organization);

  /**
   * Select all properties for a specific media package.
   *
   * @param mediaPackageId
   *          Media package identifier to check for
   * @param namespace
   *          Namespace to limit the search to
   * @return List of properties
   */
  List<Property> selectProperties(String mediaPackageId, String namespace);

  /** Create a new query builder. */
  AQueryBuilder createQuery();

  /**
   * Deserialize a version from a string. This is the inverse function of {@link Version#toString()}.
   *
   * @return a version or none, if no version can be archived from the given string
   */
  Opt<Version> toVersion(String version);
}
