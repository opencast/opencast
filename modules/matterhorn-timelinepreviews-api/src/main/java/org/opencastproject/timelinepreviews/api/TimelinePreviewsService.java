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


package org.opencastproject.timelinepreviews.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;

/**
 * Api for timeline preview generation implementations, that create preview images to be shown on the timeline of a video.
 */
public interface TimelinePreviewsService {

  /** Job type */
  String JOB_TYPE = "org.opencastproject.timelinepreviews";

  /**
   * Takes the given track and returns the job that can be used to generate timeline preview images.
   *
   * @param track
   *          track to generate preview images for
   * @param imageCount
   *          number of preview images that will be generated
   * @return the job that can generate the preview images
   * @throws TimelinePreviewsException
   *           if the timeline preview images could not be generated
   * @throws MediaPackageException
   *           if the track is invalid
   */
  Job createTimelinePreviewImages(Track track, int imageCount) throws TimelinePreviewsException, MediaPackageException;

}
