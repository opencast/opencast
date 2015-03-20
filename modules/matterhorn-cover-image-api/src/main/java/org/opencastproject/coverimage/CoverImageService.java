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
package org.opencastproject.coverimage;

import org.opencastproject.job.api.Job;

/**
 * Provides capabilities to generate a cover image
 */
public interface CoverImageService {

  String JOB_TYPE = "org.opencastproject.coverimage";

  /**
   * Creates a job for creating a cover image for movie/media package.
   *
   * @param xml
   *          metadata as XML. Will be handed over to the XSL transformation. Optional, if not given, there should be a
   *          fallback
   * @param xsl
   *          the XSL stylesheet used to create the SVG version of the cover image
   * @param width
   *          the width of the resulting image
   * @param height
   *          the height of the resulting image
   * @param posterImageUri
   *          file URI to an additional poster image (optional)
   * @param targetFlavor
   *          target flavor
   * @return a job instance
   * @throws CoverImageException
   *           if there is an error while creating the job
   */
  Job generateCoverImage(String xml, String xsl, String width, String height, String posterImageUri, String targetFlavor)
          throws CoverImageException;

}
