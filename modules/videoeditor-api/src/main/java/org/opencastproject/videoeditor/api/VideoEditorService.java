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

package org.opencastproject.videoeditor.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.smil.entity.api.Smil;

import java.util.List;

public interface VideoEditorService {

  /**
   * ServiceRegistry job type.
   */
  String JOB_TYPE = "org.opencastproject.videoeditor";

  /**
   * Create Smil processing {@link org.opencastproject.job.api.Job}s to edit Tracks.
   * Parse Smil document, extract Tracks to edit and split points where to cut.
   *
   * @param smil
   * @return Processing Jobs
   * @throws ProcessFailedException if an error occures
   */
  List<Job> processSmil(Smil smil) throws ProcessFailedException;
}
