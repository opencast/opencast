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
package org.opencastproject.distribution.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;

/**
 * Distributes elements from MediaPackages to distribution channels.
 */
public interface DistributionService {

  /**
   * A prefix used by distribution service implementations to indicate the types of distribution channels they manage.
   */
  String JOB_TYPE_PREFIX = "org.opencastproject.distribution";

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
  Job distribute(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException, MediaPackageException;

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

}
