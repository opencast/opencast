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
package org.opencastproject.fileupload.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencastproject.fileupload.api.FileUploadHandler;
import org.opencastproject.fileupload.api.FileUploadService;
import org.opencastproject.fileupload.api.exception.FileUploadException;
import org.opencastproject.fileupload.api.job.FileUploadJob;
import org.opencastproject.fileupload.api.job.Payload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A service for big file uploads via HTTP.
 * 
 * TODO make it possible to register n upload handlers instead of just one
 * TODO make compatible with ordinary HTML uploads (old method from /ingest/addElementMonitored)
 *
 */
public class FileUploadServiceImpl implements FileUploadService {

  final String PROPKEY_STORAGE_DIR = "org.opencastproject.storage.dir";
  final String DIRNAME_WORK_ROOT = "fileupload";
  final String FILENAME_DATAFILE = "payload.part";
  final String FILENAME_CHUNKFILE = "chunk.part";
  final String FILENAME_JOBFILE = "job.xml";
  private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);
  private Marshaller jobMarshaller;
  private Unmarshaller jobUnmarshaller;
  private File workRoot;
  private FileUploadHandler uploadHandler;
  private HashMap<String, FileUploadJob> jobCache = new HashMap<String, FileUploadJob>();

  // <editor-fold defaultstate="collapsed" desc="OSGi Service Stuff" >
  protected void activate(ComponentContext cc) throws Exception {
    // ensure existence of working directory
    String dirname = cc.getBundleContext().getProperty(PROPKEY_STORAGE_DIR);
    if (dirname != null) {
      workRoot = new File(dirname + File.separator + DIRNAME_WORK_ROOT);
      if (!workRoot.exists()) {
        FileUtils.forceMkdir(workRoot);
      }
    } else {
      throw new RuntimeException("Storage directory must be defined with framework property " + PROPKEY_STORAGE_DIR);
    }
    
    // set up de-/serialization
    ClassLoader cl = FileUploadJob.class.getClassLoader();
    JAXBContext jctx = JAXBContext.newInstance("org.opencastproject.fileupload.api.job", cl);
    jobMarshaller = jctx.createMarshaller();
    jobUnmarshaller = jctx.createUnmarshaller();
    
    log.info("File Upload Service activated. Storage directory is {}", workRoot.getAbsolutePath());
  }

  protected void deactivate(ComponentContext cc) {
    log.info("File Upload Service deactivated");
  }

  protected void setFileUploadHandler(FileUploadHandler uploadHandler) {
    this.uploadHandler = uploadHandler;
    log.info("Registered UploadHanlder: {}", uploadHandler.getClass().getName());
  }

  protected void unsetFileUploadHandler(FileUploadHandler uploadHandler) {
    this.uploadHandler = null;
  }
  // </editor-fold>

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#createJob(String filename, long filesize, int chunksize)
   */
  @Override
  public FileUploadJob createJob(String filename, long filesize, int chunksize) throws FileUploadException {
    FileUploadJob job = new FileUploadJob(filename, filesize, chunksize);

    try {
      File jobDir = getJobDir(job.getId());       // create working dir
      FileUtils.forceMkdir(jobDir);
      ensureExists(getPayloadFile(job.getId()));  // create empty payload file
      storeJob(job);                              // create job file
    
    } catch (FileUploadException e) {
      String message = new StringBuilder()
              .append("Could not create job file in ")
              .append(workRoot.getAbsolutePath()).append(": ")
              .append(e.getMessage()).toString();
      log.error(message, e);
      throw new FileUploadException(message, e);
      
    } catch (IOException e) {
      String message = new StringBuilder()
              .append("Could not create upload job directory in ")
              .append(workRoot.getAbsolutePath()).append(": ")
              .append(e.getMessage()).toString();
      log.error(message, e);
      throw new FileUploadException(message, e);
    }
    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#hasJob(String id)
   */
  @Override
  public boolean hasJob(String id) {
    try {
      if (isLocked(id)) {
        return true;
      } else {
        File dir = getJobDir(id);
        return (dir.exists() && dir.isDirectory());
      }
    } catch (Exception e) {
      log.warn("Error while looking for upload job: " + e.getMessage());
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#getJob(String id)
   */
  @Override
  public FileUploadJob getJob(String id) throws FileUploadException {
    try {
      if (isLocked(id)) {
        return jobCache.get(id);
      } else {
        File jobFile = getJobFile(id);
        FileUploadJob job = (FileUploadJob) jobUnmarshaller.unmarshal(jobFile);
        return job;
      }
    } catch (Exception e) {
      log.warn("Failed to load job " + id + ": " + e.getMessage());
      throw new FileUploadException("Error retrieving job " + id, e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#storeJob(org.opencastproject.fileupload.api.job.FileUploadJob job)
   */
  @Override
  public void storeJob(FileUploadJob job) throws FileUploadException {
    try {
      File jobFile = ensureExists(getJobFile(job.getId()));
      jobMarshaller.marshal(job, jobFile);
    } catch (Exception e) {
      log.warn("Error while storing upload job: " + e.getMessage());
      throw new FileUploadException("Failed to write job file.");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#deleteJob(String id)
   */  
  @Override
  public void deleteJob(String id) throws FileUploadException {
    try {
      if (isLocked(id)) {
        jobCache.remove(id);
      }
      File jobDir = new File(workRoot.getAbsolutePath() + File.separator + id);
      FileUtils.forceDelete(jobDir);
    } catch (Exception e) {
      log.warn("Error while deleting upload job: " + e.getMessage());
      throw new FileUploadException("Error deleting job", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#acceptChunk(org.opencastproject.fileupload.api.job.FileUploadJob job, long chunk, InputStream content)
   */
  @Override
  public void acceptChunk(FileUploadJob job, long chunk, InputStream content) throws FileUploadException {
    // job ready to recieve data?
    if (isLocked(job.getId())) {
      throw new FileUploadException("Job is locked. Seems like a concurrent upload to this job is in progress.");
    } else {
      lock(job);
    }
    
    // job already completed?
    if (job.getState().equals(FileUploadJob.JobState.COMPLETE)) {
      unlock(job);
      throw new FileUploadException("Job is already complete!");
    }
    
    // right chunk offered?
    int supposedChunk = job.getCurrentChunk().getNumber() + 1;
    if (chunk != supposedChunk) {
      StringBuilder sb = new StringBuilder()
              .append("Wrong chunk number! Awaiting #")
              .append(supposedChunk).append(" but #")
              .append(Long.toString(chunk)).append(" was offered.");
      unlock(job);
      throw new FileUploadException(sb.toString());
    }

    // write chunk to temp file
    File chunkFile = ensureExists(getChunkFile(job.getId()));
    OutputStream out = null;
    try {
      out = new FileOutputStream(chunkFile, false);
      IOUtils.copy(content, out);
    } catch (Exception e) {
      unlock(job);
      throw new FileUploadException("Failed to store chunk data!", e);
    } finally {
      IOUtils.closeQuietly(out);
    }

    // check if chunk has right size
    long actualSize = chunkFile.length();
    long supposedSize;
    if (chunk == job.getChunksTotal()-1) {
      supposedSize = job.getPayload().getTotalSize() % job.getChunksize();
      supposedSize = supposedSize == 0 ? job.getChunksize() : supposedSize;   // a not so nice workaround for the rare case that file size is a multiple of chunk size
    } else {
      supposedSize = job.getChunksize();
    }
    if (actualSize == supposedSize) {         
      
      // append chunk to payload file
      FileInputStream in = null;
      try {
        File payloadFile = getPayloadFile(job.getId());
        in = new FileInputStream(chunkFile);
        out = new FileOutputStream(payloadFile, true);
        IOUtils.copy(in, out);
        Payload payload = job.getPayload();
        payload.setCurrentSize(payload.getCurrentSize() + actualSize);
      
      } catch (IOException e) {
        log.error("Failed to append chunk data.", e);
        unlock(job);
        throw new FileUploadException("Could not append chunk data", e);
      
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        deleteChunkFile(job.getId());
      }
      
    } else {
      StringBuilder sb = new StringBuilder()
              .append("Chunk has wrong size. Awaited: ").append(supposedSize)
              .append(" bytes, recieved: ").append(actualSize).append(" bytes.");
      unlock(job);
      throw new FileUploadException(sb.toString());
    }

    // update job
    job.getCurrentChunk().incrementNumber();
    if (chunk == job.getChunksTotal()-1) {    // upload is complete
      log.info("Upload job completed: {}", job.getId());
      finalizeJob(job);
      notifyUploadHandler(job);
    } else {                                  // upload still in-complete
      unlock(job);
      storeJob(job);
    }
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#getPayload(org.opencastproject.fileupload.api.job.FileUploadJob job)
   */
  @Override
  public InputStream getPayload(FileUploadJob job) throws FileUploadException {
    // job ready to recieve data?
    if (isLocked(job.getId())) {
      throw new FileUploadException("Job is locked. Download is only permitted while no upload to this job is in progress.");
    } else {
      lock(job);
    }
    
    try {
      FileInputStream payload = new FileInputStream(getPayloadFile(job.getId()));
      return payload;
    } catch (FileNotFoundException e) {
      throw new FileUploadException("Failed to retrieve file from job " + job.getId());
    }
  }

  /** Locks an upload job.
   * 
   * @param job job to lock
   */
  private void lock(FileUploadJob job) {
    job.setState(FileUploadJob.JobState.INPROGRESS);
    jobCache.put(job.getId(), job);
  }
  
  /** Returns true if the job with the given ID is currently locked.
   * 
   * @param id ID of the job in question
   * @return true if job is locked, false otherwise
   */
  private boolean isLocked(String id) {
    return jobCache.containsKey(id);
  }
  
  /** Unlocks an upload job.
   * 
   * @param job job to unlock
   * @throws FileUploadException 
   */
  private void unlock(FileUploadJob job) throws FileUploadException {
    jobCache.remove(job.getId());
    job.setState(FileUploadJob.JobState.READY);
  }
  
  /** Unlocks an finalizes an upload job. 
   * 
   * @param job job to finalize
   * @throws FileUploadException 
   */
  private void finalizeJob(FileUploadJob job) throws FileUploadException {
    job.setState(FileUploadJob.JobState.COMPLETE);
    storeJob(job);
    jobCache.remove(job.getId());
  }
  
  /** Notifies the registered UploadHandler that an upload has been finished.
   * 
   * TODO extend so that more than one UploadHandler can be notified.
   * 
   * @param job job that was finished
   */
  private void notifyUploadHandler(FileUploadJob job) {
    if (this.uploadHandler != null) {
      try {
        File payload = getPayloadFile(job.getId());
        InputStream in = new FileInputStream(payload);
        if (this.uploadHandler.accept(job, in)) {       // hand over the payload, if successful:
          IOUtils.closeQuietly(in);
          deleteJob(job.getId());                       // delete job information and payload data from workspace
        } else {
          IOUtils.closeQuietly(in);
          log.warn("UploadHandler returned false on accept!");
        }
      } catch (Exception e) {
        throw new IllegalStateException("Unable to handle uploaded file.", e);
      }
    }
  }
      
  /** Deletes the chunk file from workspace.
   * 
   * @param id ID of the job of which the chunk file should be deleted
   */
  private void deleteChunkFile(String id) {
    File chunkFile = getChunkFile(id);
    try {
      if (!chunkFile.delete()) {
        throw new RuntimeException("Could not delete chunk file");
      }
    } catch (Exception e) {
      log.warn("Could not delete chunk file " + chunkFile.getAbsolutePath());
    }
  }
  
  /** Ensures the existence of a given file.
   * 
   * @param file 
   * @return File existing file 
   * @throws IllegalStateException 
   */ 
  private File ensureExists(File file) throws IllegalStateException {
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create chunk file!");
      }
    }
    return file;
  }
  
  /** Returns the directory for a given job ID.
   * 
   * @param id ID for which a directory name should be generated
   * @return File job directory
   */
  private File getJobDir(String id) {
    StringBuilder sb = new StringBuilder()
            .append(workRoot.getAbsolutePath()).append(File.separator).append(id);
    return new File(sb.toString());
  }

  /** Returns the job information file for a given job ID.
   * 
   * @param id ID for which a job file name should be generated
   * @return File job file
   */
  private File getJobFile(String id) {
    StringBuilder sb = new StringBuilder()
            .append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_JOBFILE);
    return new File(sb.toString());
  }
  
  /** Returns the chunk file for a given job ID.
   * 
   * @param id ID for which a chunk file name should be generated
   * @return File chunk file
   */
  private File getChunkFile(String id) {
    StringBuilder sb = new StringBuilder()
            .append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_CHUNKFILE);
    return new File(sb.toString());
  }
  
  /** Returns the payload file for a given job ID.
   * 
   * @param id ID for which a payload file name should be generated
   * @return File job file
   */
  private File getPayloadFile(String id) {
    StringBuilder sb = new StringBuilder()
            .append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_DATAFILE);
    return new File(sb.toString());
  }

}
