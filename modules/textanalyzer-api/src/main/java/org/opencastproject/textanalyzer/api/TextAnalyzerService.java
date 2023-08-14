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


package org.opencastproject.textanalyzer.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageException;

/**
 * Api for text analysis implementations, aimed at extracting text from an image.
 */
public interface TextAnalyzerService {

  /** Receipt type */
  String JOB_TYPE = "org.opencastproject.textanalyzer";

  /** The operation type */
  String OPERATION = "extract";

  /**
   * Takes the given image and returns a receipt that can be used to get the resulting catalog.
   *
   * @param image
   *          element to analyze
   * @return the metadata
   * @throws TextAnalyzerException
   *           if the text in this image can not be analyzed
   * @throws MediaPackageException
   *           if this attachment is not valid
   */
  Job extract(Attachment image) throws TextAnalyzerException, MediaPackageException;

}
