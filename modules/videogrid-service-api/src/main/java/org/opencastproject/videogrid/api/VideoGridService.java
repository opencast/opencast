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

package org.opencastproject.videogrid.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;

import org.apache.commons.codec.EncoderException;

import java.util.List;

/**
 * Generate a single video out of many, arranged in a grid.
 */
public interface VideoGridService {

  /**
   * The namespace distinguishing videogrid jobs from other types
   */
  String JOB_TYPE = "org.opencastproject.videogrid";

  /**
   * Generate the final video in parts
   *
   * @param command
   *          An ffmpeg command as a list
   * @param tracks
   *          Source tracks used by the ffmpeg command
   * @return VideoGrid service job.
   * @throws VideoGridServiceException
   *          If something went wrong during the processing
   */
  Job createPartialTrack(List<String> command, Track... tracks)
          throws VideoGridServiceException, EncoderException, MediaPackageException;
}
