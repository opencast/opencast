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

/**
 * Distributes elements from MediaPackages to distribution channels.
 */
public interface DistributionService {

  /**
   * A prefix used by distribution service implementations to indicate the types of distribution channels they manage.
   */
  String JOB_TYPE_PREFIX = "org.opencastproject.distribution.";

  String CONFIG_KEY_STORE_TYPE = "distribution.channel";

  /**
   * Distribute a media package element.
   *
   * @param mediapackage
   *          the media package
   * @param elementId
   *          the element in the media package to distribute
   *
   * @return The job
   * @throws DistributionException
   *           if there was a problem distributing the media
   * @throws MediaPackageException
   *           if there was a problem with the mediapackage element
   */
  Job distribute(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException, MediaPackageException;

  /**
   * Distributes a media package element synchronously, bypassing the Opencast job system.
   *
   * @param mediapackage
   *          the media package
   * @param elementId
   *          the element in the media package to distribute
   *
   * @return list of distributed media package elements
   * @throws DistributionException
   *           if there was a problem distributing the media
   * @throws MediaPackageException
   *           if there was a problem with the mediapackage element
   */
  List<MediaPackageElement> distributeSync(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException, MediaPackageException;

  /**
   * Retract a media package element from the distribution channel.
   *
   * @param mediaPackage
   *          the media package
   * @param elementId
   *          the media package element to retract
   * @throws DistributionException
   *           if there was a problem retracting the mediapackage
   */
  Job retract(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException;

  /**
   * Retract a media package element from the distribution channel synchronously, bypassing the Opencast job system.
   *
   * @return list of retracted media package elements
   * @param mediaPackage
   *          the media package
   * @param elementId
   *          the media package element to retract
   * @throws DistributionException
   *           if there was a problem retracting the mediapackage
   */
  List<MediaPackageElement> retractSync(String channelId, MediaPackage mediaPackage, String elementId)
          throws DistributionException, MediaPackageException;

  /**
   * Returns the distribution type for this service.
   * This type should be unique within an Opencast instance, and is used to select where file distribution happens.
   *
   * @return The distribution type.  A string like "download", or "streaming"
   */
  String getDistributionType();
}
