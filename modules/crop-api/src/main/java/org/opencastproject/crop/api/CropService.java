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

package org.opencastproject.crop.api;


import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;

/**
 * Api for cropping implementations.
 */
public interface CropService {
  /** Job type */
  String JOB_TYPE = "org.opencastproject.crop";

  /**
   * Takes the given track and returns the job that can be used to get the resulting mpeg7 catalog
   *
   * @param track
   *          track to crop
   * @return the job with which we can obtain the extracted metadata
   * @throws CropException
   *              if the track could not be cropped
   * @throws MediaPackageException
   *              if the track is invalid
   */
  Job crop(Track track) throws CropException, MediaPackageException;
}
