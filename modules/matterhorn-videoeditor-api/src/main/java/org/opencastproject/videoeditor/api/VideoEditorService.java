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
package org.opencastproject.videoeditor.api;

import java.util.List;
import org.opencastproject.job.api.Job;
import org.opencastproject.smil.entity.api.Smil;

public interface VideoEditorService {
    
  /**
   * Create {@see org.opencastproject.smil.entity.Smil} processing 
   * {@see org.opencastproject.job.api.Job}s to edit Tracks.
   * Parse Smil document, extract Tracks to edit and split points where to cut.
   * 
   * @param smil
   * @return Processing Jobs
   * @throws ProcessFailedException if an error occures
   */
  List<Job> processSmil(Smil smil) throws ProcessFailedException;
}
