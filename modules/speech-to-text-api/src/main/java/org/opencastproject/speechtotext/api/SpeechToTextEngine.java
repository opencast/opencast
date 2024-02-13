/*
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

/** Interface for speech-to-text implementations. */
public interface SpeechToTextEngine {

  class Result {
    private final String language;
    private final File subtitleFile;

    public Result(String language, File subtitleFile) {
      this.language = language;
      this.subtitleFile = subtitleFile;
    }

    public String getLanguage() {
      return language;
    }

    public File getSubtitleFile() {
      return subtitleFile;
    }
  }

  /**
   * Returns the name of the implemented engine.
   *
   * @return The name of the implemented engine.
   */
  String getEngineName();

  /**
   * Generates the subtitles file.
   *
   * @param mediaFile          The media package containing the audio track.
   * @param workingDirectory   A unique working directory to safely operate in.
   * @param language           The language of the audio track.
   * @param translate          If the subtitles should be translated into english
   * @return Result containing the language code and the subtitles file path.
   * @throws SpeechToTextEngineException Thrown when an error occurs at the process.
   */
  Result generateSubtitlesFile(File mediaFile, File workingDirectory, String language,
          Boolean translate) throws SpeechToTextEngineException;

}
