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

package org.opencastproject.caption.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;

/**
 * Provides captioning support. This service makes use of {@link CaptionConverter} instances that need to be registered
 * in the OSGi registry.
 */
public interface CaptionService {

  String JOB_TYPE = "org.opencastproject.caption";

  /**
   * Converts captions from one format to another. Language parameter is used for those formats that store information
   * about language.
   *
   * @param input
   *          MediaPackageElement containing captions
   * @param inputFormat
   *          format of imported captions
   * @param outputFormat
   *          format of exported captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no matching engine registered for given input or output
   * @throws CaptionConverterException
   *           if exception occurs while converting
   * @throws MediaPackageException
   *           if the catalog is invalid
   */
  Job convert(MediaPackageElement input, String inputFormat, String outputFormat)
          throws UnsupportedCaptionFormatException, CaptionConverterException, MediaPackageException;

  /**
   * Converts captions from one format to another. Language parameter is used for those formats that store information
   * about language.
   *
   * @param input
   *          MediaPackageElement containing captions
   * @param inputFormat
   *          format of imported captions
   * @param outputFormat
   *          format of exported captions
   * @param language
   *          language of captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no matching engine registered for given input or output
   * @throws CaptionConverterException
   *           if exception occurs while converting
   * @throws MediaPackageException
   *           if the catalog is invalid
   */
  Job convert(MediaPackageElement input, String inputFormat, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException, MediaPackageException;

  /**
   * Returns list of languages available in captions (if such information is stored).
   *
   * @param input
   *          Catalog containing captions
   * @param format
   *          captions' format
   * @return Array of languages available in captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no matching engine registered for given input or output
   * @throws CaptionConverterException
   *           if parser encounters exception
   */
  String[] getLanguageList(MediaPackageElement input, String format)
          throws UnsupportedCaptionFormatException, CaptionConverterException;

}
