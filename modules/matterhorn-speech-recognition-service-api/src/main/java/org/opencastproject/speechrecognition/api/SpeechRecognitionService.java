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

package org.opencastproject.speechrecognition.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Track;

import java.util.List;

/**
 * Service leveraging speech recognition to transcribe audio tracks into MPEG7 catalogs.
 */
public interface SpeechRecognitionService {

  /**
   * Start the track transcription in the given language. If the given language is null, the speech-recognition service
   * find/define it automatically.
   *
   * @param track
   *          the track to transcribe
   * @param language
   *          the language of the transcription
   * @return the job for this operation
   */
  Job transcribe(Track track, String language);

  /**
   * Return whether the speech-recognition service support the given language. The language must be in ISO-639-2 format.
   *
   * @param language
   *          the language in ISO 639-2 format.
   * @return whether the service support or not the language
   */
  boolean isLanguageAvailable(String language);

  /**
   * Gets the list of all the languages supported by the speech-recognition service.
   *
   * @return the languages list. the languages are formated in ISO 639-2.
   */
  List<String> getSupportedLanguages();

}
