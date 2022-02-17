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

import java.io.File;
import java.net.URI;

/** Interface for speech-to-text implementations. */
public interface SpeechToTextEngine {

  /**
   * Returns the name of the implemented engine.
   *
   * @return The name of the implemented engine.
   */
  String getEngineName();

  /**
   * Generates the subtitles file.
   *
   * @param mediaFile The media package containing the audio track.
   * @param preparedOutputFile The prepared output file where the subtitles data should be saved.
   * @param language The language of the audio track.
   * @return The generated subtitles file.
   * @throws SpeechToTextEngineException Thrown when an error occurs at the process.
   */
  File generateSubtitlesFile(URI mediaFile, File preparedOutputFile, String language)
          throws SpeechToTextEngineException;

}
