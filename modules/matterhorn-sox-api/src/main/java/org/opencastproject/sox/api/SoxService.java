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
package org.opencastproject.sox.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;

/**
 * Provides sound processing services with SoX
 */
public interface SoxService {

  String JOB_TYPE = "org.opencastproject.sox";

  /**
   * Get audio statistics, using that track's audio streams.
   *
   * @param sourceAudioTrack
   *          The source audio track
   * @return The receipt for this audio processing job.
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   * @throws SoxException
   *           if audio processing fails
   */
  Job analyze(Track sourceAudioTrack) throws MediaPackageException, SoxException;

  /**
   * Normalize the audio stream of that track.
   *
   * @param sourceAudioTrack
   *          The source audio track
   * @param targetRmsLevDb
   *          The target RMS Lev dB
   * @return The receipt for this audio processing job
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   * @throws SoxException
   *           if audio processing fails
   */
  Job normalize(Track sourceAudioTrack, Float targetRmsLevDb) throws MediaPackageException, SoxException;

}
