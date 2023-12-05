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


package org.opencastproject.inspection.ffmpeg.api;


import java.io.File;
import java.util.Map;

/**
 * Interface for tools that analyze media files.
 * Implement this and register it as a factory service.
 *
 * MediaAnalyzers are not guaranteed to be thread safe.
 */
public interface MediaAnalyzer {

  /**
   * Analyze a media file and return the metadata that is found
   *
   * @param media any media file
   * @return the metadata that is found
   * @throws MediaAnalyzerException if the analyzer fails
   */
  MediaContainerMetadata analyze(File media) throws MediaAnalyzerException;

  /*
   * Needed for https://issues.opencastproject.org/jira/browse/MH-2157 -AZ
   */
  void setConfig(Map<String, Object> config);

}
