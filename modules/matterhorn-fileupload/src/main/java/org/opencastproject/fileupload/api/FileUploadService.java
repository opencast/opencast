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
package org.opencastproject.fileupload.api;

import org.opencastproject.fileupload.api.exception.FileUploadException;
import org.opencastproject.fileupload.api.job.FileUploadJob;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for a Service that manages upload jobs and recieves and handles file parts.
 * 
 */
public interface FileUploadService {

  /**
   * Returns true is a job with the given ID exists.
   * 
   * @param id
   *          ID of the job in question
   * @return true if job exists, false otherwise
   */
  boolean hasJob(String id);

  /**
   * Creates a new upload job with the given metadata.
   * 
   * @param filename
   *          name of the file to be uploaded
   * @param fileSize
   *          size of the file
   * @param chunkSize
   *          size of the file parts that will be uploaded
   * @param mp
   *          the mediapackage this file should belong to
   * @return FileUploadJob the job object
   * @throws FileUploadException
   */
  FileUploadJob createJob(String filename, long fileSize, int chunkSize, MediaPackage mp,
          MediaPackageElementFlavor flavor) throws FileUploadException;

  /**
   * Returns the upload job with the given ID, throws <code>FileUploadException</code> if the job can not be found.
   * 
   * @param id
   *          ID of the upload job to retrieve
   * @return FileUploadJob the job object in question
   * @throws FileUploadException
   */
  FileUploadJob getJob(String id) throws FileUploadException;

  /**
   * Cleans outdated jobs on the file system
   */
  void cleanOutdatedJobs() throws IOException;

  /**
   * Persists the given job object.
   * 
   * @param job
   *          job object to persist
   * @throws FileUploadException
   */
  void storeJob(FileUploadJob job) throws FileUploadException;

  /**
   * Deletes the job permanently, thus deleting persistent data.
   * 
   * @param id
   *          ID of the upload job to delete
   * @throws FileUploadException
   */
  void deleteJob(String id) throws FileUploadException;

  /**
   * Appends the next part to the payload and updates the upload job accordingly.
   * 
   * @param job
   *          the job object for the upload
   * @param chunk
   *          the number of the chunk being transfered
   * @param content
   *          the actual payload data
   * @throws FileUploadException
   */
  void acceptChunk(FileUploadJob job, long chunk, InputStream content) throws FileUploadException;

  /**
   * Returns an <code>InputStream</code> containing the data from the payload.
   * 
   * @param job
   *          job to retrieve payload data from
   * @return InputStream the payload data
   * @throws FileUploadException
   */
  InputStream getPayload(FileUploadJob job) throws FileUploadException;

}
