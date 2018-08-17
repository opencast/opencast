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
package org.opencastproject.waveform.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;

/**
 * This is an api for a service that will create a waveform image from a track.
 */
public interface WaveformService {

  /** Job type */
  String JOB_TYPE = "org.opencastproject.waveform";

  /**
   * Takes the given track and returns the job that will create a waveform image.
   *
   * @param sourceTrack the track to create waveform image from
   * @return a job that will create a waveform image
   * @throws MediaPackageException if the serialization of the given track fails
   * @throws WaveformServiceException if the job can't be created for any reason
   */
  Job createWaveformImage(Track sourceTrack) throws MediaPackageException, WaveformServiceException;
}
