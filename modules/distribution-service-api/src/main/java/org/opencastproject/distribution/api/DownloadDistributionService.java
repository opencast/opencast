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

package org.opencastproject.distribution.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;

import java.util.List;
import java.util.Set;

/**
 * Distributes elements from MediaPackages to distribution channels.
 */
public interface DownloadDistributionService extends DistributionService {

  Job distribute(String channelId, MediaPackage mediapackage, String elementId, boolean checkAvailability)
          throws DistributionException, MediaPackageException;

  Job distribute(String channelId, MediaPackage mediapackage, Set<String> elementIds, boolean checkAvailability)
          throws DistributionException, MediaPackageException;

  Job distribute(String pubChannelId, MediaPackage mediaPackage, Set<String> downloadIds, boolean checkAvailability, boolean preserveReference)
           throws DistributionException, MediaPackageException;

  Job retract(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
          throws DistributionException;

  /**
   * Distributes the given elements synchronously. This should be used rarely since load balancing will be unavailable.
   * However, since the dispatching logic is bypassed, synchronous execution is much faster. It is useful in interactive
   * scenarios where you synchronously wait for job execution anyway and you don't want to make the user waiting for too
   * long.
   *
   * @param channelId The channel to retract from.
   * @param mediapackage A media package holding the elements to retract.
   * @param elementIds The IDs of the elements to retract.
   * @param checkAvailability Whether to check if the distributed elements are available through their URI.
   *
   * @return The distributed elements.
   * @throws DistributionException In case distribution fails.
   */
  List<MediaPackageElement> distributeSync(String channelId, MediaPackage mediapackage, Set<String> elementIds, boolean checkAvailability)
      throws DistributionException;

  /**
   * Retracts the given elements synchronously. This should be used rarely since load balancing will be unavailable.
   * However, since the dispatching logic is bypassed, synchronous execution is much faster. It is useful in interactive
   * scenarios where you synchronously wait for job execution anyway and you don't want to make the user waiting for too
   * long.
   *
   * @param channelId The channel to retract from.
   * @param mediaPackage A media package holding the elements to retract.
   * @param elementIds The IDs of the elements to retract.
   *
   * @return The retracted elements.
   *
   * @throws DistributionException In case retraction fails.
   */
  List<MediaPackageElement> retractSync(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
      throws DistributionException;
}
