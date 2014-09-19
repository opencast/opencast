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

import static java.lang.String.format;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;

import org.opencastproject.fileupload.api.FileUploadService;
import org.opencastproject.fileupload.api.exception.FileUploadException;
import org.opencastproject.fileupload.api.job.Chunk;
import org.opencastproject.fileupload.api.job.FileUploadJob;
import org.opencastproject.fileupload.api.job.Payload;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/** A service for big file uploads via HTTP. */
public class FileUploadServiceImpl implements FileUploadService, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceImpl.class);
  final String PROPKEY_STORAGE_DIR = "org.opencastproject.storage.dir";
  final String PROPKEY_CLEANER_MAXTTL = "org.opencastproject.upload.cleaner.maxttl";
  final String PROPKEY_UPLOAD_WORKDIR = "org.opencastproject.upload.workdir";
  final String DEFAULT_UPLOAD_WORKDIR = "fileupload-tmp"; /* The default location is the storage dir */
  final String UPLOAD_COLLECTION = "uploaded";
  final String FILEEXT_DATAFILE = ".payload";
  final String FILENAME_CHUNKFILE = "chunk.part";
  final String FILENAME_JOBFILE = "job.xml";
  final int READ_BUFFER_LENGTH = 512;
  final int DEFAULT_CLEANER_MAXTTL = 6;
  private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);
  private File workRoot = null;
  private IngestService ingestService;
  private Workspace workspace;
  private Marshaller jobMarshaller;
  private Unmarshaller jobUnmarshaller;
  private HashMap<String, FileUploadJob> jobCache = new HashMap<String, FileUploadJob>();
  private byte[] readBuffer = new byte[READ_BUFFER_LENGTH];
  private FileUploadServiceCleaner cleaner;
  private int jobMaxTTL = DEFAULT_CLEANER_MAXTTL;

  // <editor-fold defaultstate="collapsed" desc="OSGi Service Stuff" >
  protected synchronized void activate(ComponentContext cc) throws Exception {
    /* Ensure a working directory is set */
    if (workRoot == null) {
      /* Use the default location: STORAGE_DIR / DEFAULT_UPLOAD_WORKDIR */
      String dir = cc.getBundleContext().getProperty(PROPKEY_STORAGE_DIR);
      if (dir == null) {
        throw new RuntimeException("Storage directory not defined. " + "Use " + PROPKEY_STORAGE_DIR
                + " to set the property.");
      }
      dir += File.separator + DEFAULT_UPLOAD_WORKDIR;
      workRoot = new File(dir);
      log.info("Storage directory set to {}.", workRoot.getAbsolutePath());
    }

    // set up de-/serialization
    ClassLoader cl = FileUploadJob.class.getClassLoader();
    JAXBContext jctx = JAXBContext.newInstance("org.opencastproject.fileupload.api.job", cl);
    jobMarshaller = jctx.createMarshaller();
    jobUnmarshaller = jctx.createUnmarshaller();

    cleaner = new FileUploadServiceCleaner(this);
    cleaner.schedule();

    log.info("File Upload Service activated.");
  }

  protected void deactivate(ComponentContext cc) {
    log.info("File Upload Service deactivated");
    cleaner.shutdown();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    // try to get time-to-live threshold for jobs, use default if not configured
    String dir = (String) properties.get(PROPKEY_UPLOAD_WORKDIR);
    if (dir != null) {
      workRoot = new File(dir);
      log.info("Configuration updated. Upload working directory set to {}.", dir);
    }
    try {
      jobMaxTTL = Integer.parseInt(((String) properties.get(PROPKEY_CLEANER_MAXTTL)).trim());
    } catch (Exception e) {
      jobMaxTTL = DEFAULT_CLEANER_MAXTTL;
      log.warn("Unable to update configuration. {}", e.getMessage());
    }
    log.info("Configuration updated. Jobs older than {} hours are deleted.", jobMaxTTL);
  }

  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  protected void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  // </editor-fold>

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.fileupload.api.FileUploadService#createJob(String, long, int,
   *      org.opencastproject.mediapackage.MediaPackage, org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public FileUploadJob createJob(String filename, long filesize, int chunksize, MediaPackage mp,
          MediaPackageElementFlavor flavor) throws FileUploadException {
    FileUploadJob job = new FileUploadJob(filename, filesize, chunksize, mp, flavor);
    log.info("Creating new upload job: {}", job);

    try {
      File jobDir = getJobDir(job.getId()); // create working dir
      FileUtils.forceMkdir(jobDir);
      ensureExists(getPayloadFile(job.getId())); // create empty payload file
      storeJob(job); // create job file
    } catch (FileUploadException e) {
      deleteJob(job.getId());
      throw fileUploadException(Severity.error, "Could not create job file in " + workRoot.getAbsolutePath(), e);
    } catch (IOException e) {
      deleteJob(job.getId());
      throw fileUploadException(Severity.error,
              "Could not create upload job directory in " + workRoot.getAbsolutePath(), e);
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
      if (jobCache.containsKey(id)) {
        return true;
      } else {
        File jobFile = getJobFile(id);
        return jobFile.exists();
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
    if (jobCache.containsKey(id)) { // job already cached?
      return jobCache.get(id);
    } else { // job not in cache?
      try { // try to load job from filesystem
        synchronized (this) {
          File jobFile = getJobFile(id);
          FileUploadJob job = (FileUploadJob) jobUnmarshaller.unmarshal(jobFile);
          job.setLastModified(jobFile.lastModified()); // get last modified time from job file
          return job;
        } // if loading from fs also fails
      } catch (Exception e) { // we could not find the job and throw an Exception
        throw fileUploadException(Severity.warn, "Failed to load job " + id + " from file.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.fileupload.api.FileUploadService#cleanOutdatedJobs()
   */
  @Override
  public void cleanOutdatedJobs() throws IOException {
    File[] workRootFiles = workRoot.listFiles();
    if (workRootFiles == null) {
      logger.trace("No outdated files found in {}", workRoot.getAbsolutePath());
      return;
    }
    for (File dir : workRoot.listFiles()) {
      if (dir.getParentFile().equals(workRoot) && dir.isDirectory()) {
        try {
          String id = dir.getName(); // assuming that the dir name is the ID of a job..
          if (!isLocked(id)) { // ..true if not in cache or job is in cache and not locked
            FileUploadJob job = getJob(id);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, -jobMaxTTL);
            if (job.lastModified() < cal.getTimeInMillis()) {
              FileUtils.forceDelete(dir);
              jobCache.remove(id);
              log.info("Deleted outdated job {}", id);
            }
          }
        } catch (Exception e) { // something went wrong, so we assume the dir is corrupted
          FileUtils.forceDelete(dir); // ..and delete it right away
          log.info("Deleted corrupted job {}", dir.getName());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.fileupload.api.FileUploadService#storeJob(org.opencastproject.fileupload.api.job.FileUploadJob
   *      job)
   */
  @Override
  public void storeJob(FileUploadJob job) throws FileUploadException {
    try {
      log.debug("Attempting to store job {}", job.getId());
      File jobFile = ensureExists(getJobFile(job.getId()));
      jobMarshaller.marshal(job, jobFile);
    } catch (Exception e) {
      throw fileUploadException(Severity.error, "Failed to write job file.", e);
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
      log.debug("Attempting to delete job " + id);
      if (isLocked(id)) {
        jobCache.remove(id);
      }
      File jobDir = getJobDir(id);
      FileUtils.forceDelete(jobDir);
    } catch (Exception e) {
      throw fileUploadException(Severity.error, "Error deleting job", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.fileupload.api.FileUploadService#acceptChunk(org.opencastproject.fileupload.api.job.FileUploadJob
   *      job, long chunk, InputStream content)
   */
  @Override
  public void acceptChunk(FileUploadJob job, long chunkNumber, InputStream content) throws FileUploadException {
    // job already completed?
    if (job.getState().equals(FileUploadJob.JobState.COMPLETE)) {
      removeFromCache(job);
      throw fileUploadException(Severity.warn, "Job is already complete.");
    }

    // job ready to recieve data?
    if (isLocked(job.getId())) {
      throw fileUploadException(Severity.error,
              "Job is locked. Seems like a concurrent upload to this job is in progress.");
    } else {
      lock(job);
    }

    // right chunk offered?
    int supposedChunk = job.getCurrentChunk().getNumber() + 1;
    if (chunkNumber != supposedChunk) {
      removeFromCache(job);
      throw fileUploadException(Severity.error,
              format("Wrong chunk number. Awaiting #%d but #%d was offered.", supposedChunk, chunkNumber));
    }
    log.debug("Receiving chunk #" + chunkNumber + " of job {}", job);

    // write chunk to temp file
    job.getCurrentChunk().incrementNumber();
    File chunkFile = null;
    try {
      chunkFile = ensureExists(getChunkFile(job.getId()));
    } catch (IOException e) {
      throw fileUploadException(Severity.error, "Cannot create chunk file", e);
    }
    OutputStream out = null;
    try {
      out = new FileOutputStream(chunkFile, false);
      int bytesRead = 0;
      long bytesReadTotal = 0l;
      Chunk currentChunk = job.getCurrentChunk(); // copy manually (instead of using IOUtils.copy()) so we can count the
      // number of bytes
      do {
        bytesRead = content.read(readBuffer);
        if (bytesRead > 0) {
          out.write(readBuffer, 0, bytesRead);
          bytesReadTotal += bytesRead;
          currentChunk.setRecieved(bytesReadTotal);
        }
      } while (bytesRead != -1);
      if (job.getPayload().getTotalSize() == -1 && job.getChunksTotal() == 1) { // set totalSize in case of ordinary
        // from submit
        job.getPayload().setTotalSize(bytesReadTotal);
      }
    } catch (Exception e) {
      removeFromCache(job);
      throw fileUploadException(Severity.error, "Failed to store chunk data", e);
    } finally {
      IOUtils.closeQuietly(content);
      IOUtils.closeQuietly(out);
    }

    // check if chunk has right size
    long actualSize = chunkFile.length();
    long supposedSize;
    if (chunkNumber == job.getChunksTotal() - 1) {
      supposedSize = job.getPayload().getTotalSize() % job.getChunksize();
      supposedSize = supposedSize == 0 ? job.getChunksize() : supposedSize; // a not so nice workaround for the rare
      // case that file size is a multiple of the
      // chunk size
    } else {
      supposedSize = job.getChunksize();
    }
    if (actualSize == supposedSize || (job.getChunksTotal() == 1 && job.getChunksize() == -1)) {

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
        removeFromCache(job);
        throw fileUploadException(Severity.error, "Failed to append chunk data", e);

      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        deleteChunkFile(job.getId());
      }

    } else {
      removeFromCache(job);
      throw fileUploadException(Severity.warn,
              format("Chunk has wrong size. Awaited: %d bytes, received: %d bytes.", supposedSize, actualSize));
    }

    // update job
    if (chunkNumber == job.getChunksTotal() - 1) { // upload is complete
      finalizeJob(job);
      log.info("Upload job completed: {}", job);
    } else {
      job.setState(FileUploadJob.JobState.READY); // upload still incomplete
    }
    storeJob(job);
    removeFromCache(job);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.fileupload.api.FileUploadService#getPayload(org.opencastproject.fileupload.api.job.FileUploadJob
   *      job)
   */
  @Override
  public InputStream getPayload(FileUploadJob job) throws FileUploadException {
    // job not locked?
    if (isLocked(job.getId())) {
      throw fileUploadException(Severity.warn,
              "Job is locked. Download is only permitted while no upload to this job is in progress.");
    }

    try {
      FileInputStream payload = new FileInputStream(getPayloadFile(job.getId()));
      return payload;
    } catch (FileNotFoundException e) {
      throw fileUploadException(Severity.error, "Failed to retrieve file from job " + job.getId(), e);
    }
  }

  /**
   * Locks an upload job and puts it in job cache.
   *
   * @param job
   *          job to lock
   */
  private void lock(FileUploadJob job) {
    jobCache.put(job.getId(), job);
    job.setState(FileUploadJob.JobState.INPROGRESS);
  }

  /**
   * Returns true if the job with the given ID is currently locked.
   *
   * @param id
   *          ID of the job in question
   * @return true if job is locked, false otherwise
   */
  private boolean isLocked(String id) {
    if (jobCache.containsKey(id)) {
      FileUploadJob job = jobCache.get(id);
      return job.getState().equals(FileUploadJob.JobState.INPROGRESS)
              || job.getState().equals(FileUploadJob.JobState.FINALIZING);
    } else {
      return false;
    }
  }

  /**
   * Removes upload job from job cache.
   *
   * @param job
   *          job to remove from cache
   */
  private void removeFromCache(FileUploadJob job) {
    jobCache.remove(job.getId());
  }

  /**
   * Unlocks an finalizes an upload job.
   *
   * @param job
   *          job to finalize
   * @throws FileUploadException
   */
  private void finalizeJob(FileUploadJob job) throws FileUploadException {
    job.setState(FileUploadJob.JobState.FINALIZING);

    if (job.getPayload().getMediaPackage() == null) { // do we have a target mediaPackge ?
      job.getPayload().setUrl(putPayloadIntoCollection(job)); // if not, put file into upload collection in WFR
    } else {
      job.getPayload().setUrl(putPayloadIntoMediaPackage(job)); // else add file to target MP
    }
    deletePayloadFile(job.getId()); // delete payload in temp directory

    job.setState(FileUploadJob.JobState.COMPLETE);
  }

  /** Function that writes the given file to the uploaded collection. */
  private Function2<InputStream, File, Option<URI>> putInCollection = new Function2<InputStream, File, Option<URI>>() {

    @Override
    public Option<URI> apply(InputStream is, File f) {
      try {
        URI uri = workspace.putInCollection(UPLOAD_COLLECTION, f.getName(), is); // storing file with jod id as name
        // instead of original filename to
        // avoid collisions (original filename
        // can be obtained from upload job)
        return Option.some(uri);
      } catch (IOException e) {
        log.error("Could not add file to collection.", e);
        return Option.none();
      }
    }
  };

  /**
   * Puts the payload of an upload job into the upload collection in the WFR and returns the URL to the file in the WFR.
   *
   * @param job
   * @return URL of the file in the WFR
   * @throws FileUploadException
   */
  private URL putPayloadIntoCollection(FileUploadJob job) throws FileUploadException {
    log.info("Moving payload of job " + job.getId() + " to collection " + UPLOAD_COLLECTION);
    Option<URI> result = IoSupport.withFile(getPayloadFile(job.getId()), putInCollection).flatMap(
            Functions.<Option<URI>> identity());
    if (result.isSome()) {
      try {
        return result.get().toURL();
      } catch (MalformedURLException e) {
        throw fileUploadException(Severity.error, "Unable to return URL of payloads final destination.", e);
      }
    } else {
      throw fileUploadException(Severity.error, "Failed to put payload in collection.");
    }
  }

  /**
   * Puts the payload of an upload job into a MediaPackage in the WFR, adds the files as a track to the MediaPackage and
   * returns the files URL in the WFR.
   *
   * @param job
   * @return URL of the file in the WFR
   * @throws FileUploadException
   */
  private URL putPayloadIntoMediaPackage(FileUploadJob job) throws FileUploadException {
    MediaPackage mediaPackage = job.getPayload().getMediaPackage();
    MediaPackageElementFlavor flavor = job.getPayload().getFlavor();
    List<Track> excludeTracks = Arrays.asList(mediaPackage.getTracks(flavor));

    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(getPayloadFile(job.getId()));
      MediaPackage mp = ingestService.addTrack(fileInputStream, job.getPayload().getFilename(), job.getPayload()
              .getFlavor(), mediaPackage);

      List<Track> tracks = new ArrayList<Track>(Arrays.asList(mp.getTracks(flavor)));
      tracks.removeAll(excludeTracks);
      if (tracks.size() != 1)
        throw new FileUploadException("Ingested track not found");

      return tracks.get(0).getURI().toURL();
    } catch (Exception e) {
      throw fileUploadException(Severity.error, "Failed to add payload to MediaPackage.", e);
    } finally {
      IOUtils.closeQuietly(fileInputStream);
    }
  }

  /**
   * Deletes the chunk file from working directory.
   *
   * @param id
   *          ID of the job of which the chunk file should be deleted
   */
  private void deleteChunkFile(String id) {
    final File chunkFile = getChunkFile(id);
    log.debug("Attempting to delete chunk file of job " + id);
    if (!chunkFile.delete()) {
      log.warn("Could not delete chunk file " + chunkFile.getAbsolutePath());
    }
  }

  /**
   * Deletes the payload file from working directory.
   *
   * @param id
   *          ID of the job of which the chunk file should be deleted
   */
  private void deletePayloadFile(String id) {
    final File payloadFile = getPayloadFile(id);
    log.debug("Attempting to delete payload file of job " + id);
    if (!payloadFile.delete()) {
      log.warn("Could not delete payload file " + payloadFile.getAbsolutePath());
    }
  }

  /** Ensures the existence of a given file. */
  private File ensureExists(File file) throws IOException {
    file.createNewFile();
    return file;
  }

  /**
   * Returns the directory for a given job ID.
   *
   * @param id
   *          ID for which a directory name should be generated
   * @return File job directory
   */
  private File getJobDir(String id) {
    final StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id);
    return new File(sb.toString());
  }

  /**
   * Returns the job information file for a given job ID.
   *
   * @param id
   *          ID for which a job file name should be generated
   * @return File job file
   */
  private File getJobFile(String id) {
    final StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_JOBFILE);
    return new File(sb.toString());
  }

  /**
   * Returns the chunk file for a given job ID.
   *
   * @param id
   *          ID for which a chunk file name should be generated
   * @return File chunk file
   */
  private File getChunkFile(String id) {
    final StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_CHUNKFILE);
    return new File(sb.toString());
  }

  /**
   * Returns the payload file for a given job ID.
   *
   * @param id
   *          ID for which a payload file name should be generated
   * @return File job file
   */
  private File getPayloadFile(String id) {
    final StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(id).append(FILEEXT_DATAFILE);
    return new File(sb.toString());
  }

  private enum Severity {
    warn, error
  }

  private FileUploadException fileUploadException(Severity severity, String msg) throws FileUploadException {
    switch (severity) {
      case warn:
        log.warn(msg);
        break;
      case error:
        log.error(msg);
        break;
      default:
        unexhaustiveMatch();
    }
    throw new FileUploadException(msg);
  }

  private FileUploadException fileUploadException(Severity severity, String msg, Exception cause)
          throws FileUploadException {
    switch (severity) {
      case warn:
        log.warn(msg, cause);
        break;
      case error:
        log.error(msg, cause);
        break;
      default:
        unexhaustiveMatch();
    }
    throw new FileUploadException(msg, cause);
  }
}
