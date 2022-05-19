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

package org.opencastproject.speechtotext.api;

import org.opencastproject.job.api.Job;

import java.net.URI;

/**
 * Generates subtitles files from video or audio sources.
 */
public interface SpeechToTextService {

  /**
   * The namespace distinguishing speech-to-text jobs from other types.
   */
  String JOB_TYPE = "org.opencastproject.speechtotext";

  /**
   * Generates a subtitles file for a media package with an audio track.
   *
   * @param mediaFile Location of the media file to generate subtitles for.
   * @param language The language of the audio.
   * @return SpeechToText service job.
   * @throws SpeechToTextServiceException If something went wrong during the subtitles generation.
   */
  Job transcribe(URI mediaFile, String language) throws SpeechToTextServiceException;

}
