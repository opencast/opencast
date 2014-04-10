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

package org.opencastproject.silencedetection.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Track;

/**
 * SilenceDetectionService detect silent seqences in audio tracks.
 */
public interface SilenceDetectionService {
  
  /**
   * ServiceRegistry job type.
   */
  String JOB_TYPE = "org.opencastproject.silencedetection";
  
  /**
   * Run silence detection on audio (visual) file.
   * 
   * @param sourceTrack track to detect non silent segments from
   * @return Job detection job
   * @throws SilenceDetectionFailedException if fails
   */ 
  Job detect(Track sourceTrack) throws SilenceDetectionFailedException;
  
  /**
   * Run silence detection on audio (visual) file.
   * 
   * @param sourceTrack track to detect non silent segments from
   * @param referenceTracks tracks to reference in smil file instead of sourceTrack
   * @return Job detection job
   * @throws SilenceDetectionFailedException if fails
   */ 
  Job detect(Track sourceTrack, Track[] referenceTracks) throws SilenceDetectionFailedException;
}
