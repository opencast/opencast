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

package org.opencastproject.textextractor.api;

import java.io.File;
import java.io.IOException;

/**
 * Interface for implementations that are able to extract text from an image.
 */
public interface TextExtractor {

  /**
   * Extracts text from the image and returns it as a set of lines in the text frame.
   *
   * @param image
   *          the image
   * @return the text
   * @throws IOException
   *           if the file can't be read
   * @throws TextExtractorException
   *           if text extraction fails
   */
  TextFrame extract(File image) throws IOException, TextExtractorException;

}
