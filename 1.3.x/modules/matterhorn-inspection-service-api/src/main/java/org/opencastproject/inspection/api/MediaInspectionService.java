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
package org.opencastproject.inspection.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;

import java.net.URI;

/**
 * Anayzes media to determine its technical metadata.
 */
public interface MediaInspectionService {

  /**
   * The namespace distinguishing media inspection jobs from other types
   */
  String JOB_TYPE = "org.opencastproject.inspection";

  /**
   * Inspect a track based on a given uri to the track and put the gathered data into the track
   * 
   * @param uri
   *          the uri to a track in a media package
   * @return the receipt of this job, that can be used to check the current status of inspect method and retrieve track
   *         with added metadata when done
   * @throws IllegalStateException
   *           if the analyzer cannot be loaded
   * @throws MediaInspectionException
   *           if there is a failure during media package update
   */
  Job inspect(URI uri) throws MediaInspectionException;

  /**
   * Equip an existing media package element with automatically generated metadata
   * 
   * @param original
   *          The original media package element that will be inspected
   * @param override
   *          In case of conflict between existing and automatically obtained metadata this switch selects preference.
   *          False..The original metadata will be kept, True..The new metadata will be used.
   * @return the receipt of this job, that can be used to check the current status of enrich method and retrieve
   *         enriched element when done
   * @throws IllegalStateException
   *           if the analyzer cannot be loaded
   * @throws MediaInspectionException
   *           if there is a failure during media package update
   * @throws MediaPackageException
   *           if the element is invalid
   */
  Job enrich(MediaPackageElement original, boolean override) throws MediaInspectionException,
          MediaPackageException;

}
