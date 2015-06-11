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

package org.opencastproject.composer.api;

import java.io.File;
import java.util.Map;

/**
 * Interface for embedding engines.
 *
 */
public interface EmbedderEngine {

  /**
   * Creates soft subtitles/captions in given media source from given caption source. Throws {@link EmbedderException}
   * if embedding is unsuccessful.
   *
   * @param mediaSource
   *          media in which captions will be embedded
   * @param captionSources
   *          source(s) of captions
   * @param captionLanguages
   *          corresponding language codes
   * @param properties
   *          additional properties that define embedding properties
   * @return media file with subtitles/captions
   * @throws EmbedderException
   *           if embedding fails
   */
  File embed(File mediaSource, File[] captionSources, String[] captionLanguages, Map<String, String> properties)
          throws EmbedderException;
}
