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
package org.opencastproject.fileupload.rest;

import org.apache.commons.fileupload.ProgressListener;
import org.opencastproject.fileupload.api.job.FileUploadJob;

/** An UploadProgressListener for updating information about upload progress to
 *  an in-memory object.
 *
 */
public class UploadProgressListener implements ProgressListener {
  
  private FileUploadJob job;

  public UploadProgressListener(FileUploadJob job) {
    this.job = job;
  }

  /**
   * Called by ServeletFileUpload on upload progress. Updates the job object.
   * 
   * @param rec number of bytes received
   * @param total number of bytes in total
   * @param i number of the current file item
   */
  @Override
  public void update(long rec, long total, int i) {
    job.getCurrentChunk().setRecieved(rec);
  }
}
