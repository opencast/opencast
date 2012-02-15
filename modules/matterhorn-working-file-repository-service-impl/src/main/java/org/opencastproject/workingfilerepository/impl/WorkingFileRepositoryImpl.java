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
package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.PathMappable;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory using the media package
 * ID as a subdirectory and the media package element ID as the file name.
 */
public class WorkingFileRepositoryImpl implements WorkingFileRepository, PathMappable {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl.class);

  /** The extension we use for the md5 hash calculated from the file contents */
  public static final String MD5_EXTENSION = ".md5";

  /** The filename filter matching .md5 files */
  private static final FilenameFilter MD5_FINAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return name.endsWith(MD5_EXTENSION);
    }
  };

  /** The remote service manager */
  protected ServiceRegistry remoteServiceManager;

  /** The root directory for storing files */
  protected String rootDirectory = null;

  /** The Base URL for this server */
  protected String serverUrl = null;

  /** The URL for the services provided by the working file repository */
  protected URI serviceUrl = null;

  /**
   * Activate the component
   */
  public void activate(ComponentContext cc) throws IOException {
    if (rootDirectory != null)
      return; // If the root directory was set, respect that setting

    // server url
    serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    if (StringUtils.isBlank(serverUrl))
      throw new IllegalStateException("Server URL must be set");

    // working file repository 'facade' configuration
    String servicePath = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    String canonicalFileRepositoryUrl = cc.getBundleContext().getProperty("org.opencastproject.file.repo.url");
    if (StringUtils.isNotBlank(canonicalFileRepositoryUrl)) {
      try {
        this.serviceUrl = new URI(UrlSupport.concat(canonicalFileRepositoryUrl, servicePath));
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Service URL must be a valid URI, but is " + canonicalFileRepositoryUrl, e);
      }
    } else {
      try {
        serviceUrl = new URI(UrlSupport.concat(serverUrl, servicePath));
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Service URL can not be set to " + serverUrl + servicePath, e);
      }
    }

    // root directory
    if (cc.getBundleContext().getProperty("org.opencastproject.file.repo.path") == null) {
      String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
      if (storageDir == null)
        throw new IllegalStateException("Storage directory must be set");
      rootDirectory = storageDir + File.separator + "opencast" + File.separator + "workingfilerepo";
    } else {
      rootDirectory = cc.getBundleContext().getProperty("org.opencastproject.file.repo.path");
    }

    try {
      createRootDirectory();
    } catch (IOException e) {
      logger.error("Unable to create the working file repository root directory at {}", rootDirectory);
      throw e;
    }

    logger.info(getDiskSpace());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#delete(java.lang.String, java.lang.String)
   */
  public boolean delete(String mediaPackageID, String mediaPackageElementID) throws IOException {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File f = getFile(mediaPackageID, mediaPackageElementID);
    if (f == null) {
      logger.info("Unable to delete non existing object {}/{}", mediaPackageID, mediaPackageElementID);
      return false;
    }
    File parentDirectory = f.getParentFile();
    logger.debug("Attempting to delete {}", parentDirectory.getAbsolutePath());
    FileUtils.forceDelete(parentDirectory);
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#get(java.lang.String, java.lang.String)
   */
  public InputStream get(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File f = getFile(mediaPackageID, mediaPackageElementID);
    if (f == null)
      throw new NotFoundException("Unable to locate " + f + " in the working file repository");
    logger.debug("Attempting to read file {}", f.getAbsolutePath());
    return new FileInputStream(f);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionURI(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public URI getCollectionURI(String collectionID, String fileName) {
    try {
      return new URI(serviceUrl + COLLECTION_PATH_PREFIX + collectionID + "/" + PathSupport.toSafeName(fileName));
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Unable to create valid uri from " + collectionID + " and " + fileName);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getURI(java.lang.String, java.lang.String)
   */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return getURI(mediaPackageID, mediaPackageElementID, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getURI(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID, String fileName) {
    String uri = UrlSupport.concat(new String[] { getBaseUri().toString(), MEDIAPACKAGE_PATH_PREFIX, mediaPackageID,
            mediaPackageElementID });
    if (fileName == null) {
      File existingDirectory = getElementDirectory(mediaPackageID, mediaPackageElementID);
      if (existingDirectory.isDirectory()) {
        File[] files = existingDirectory.listFiles();
        boolean md5Exists = false;
        for (File f : files) {
          if (f.getName().endsWith(MD5_EXTENSION)) {
            md5Exists = true;
          } else {
            fileName = f.getName();
          }
        }
        if (md5Exists && fileName != null) {
          uri = UrlSupport.concat(uri, PathSupport.toSafeName(fileName));
        }
      }
    } else {
      uri = UrlSupport.concat(uri, PathSupport.toSafeName(fileName));
    }
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#put(java.lang.String, java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in)
          throws IOException {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File f = null;
    File dir = getElementDirectory(mediaPackageID, mediaPackageElementID);
    if (dir.exists()) {
      // clear the directory
      File[] filesToDelete = dir.listFiles();
      if (filesToDelete != null && filesToDelete.length > 0) {
        for (File fileToDelete : filesToDelete) {
          if (!fileToDelete.delete()) {
            throw new IllegalStateException("Unable to delete file: " + fileToDelete.getAbsolutePath());
          }
        }
      }
    } else {
      logger.debug("Attempting to create a new directory at {}", dir.getAbsolutePath());
      FileUtils.forceMkdir(dir);
    }
    f = new File(dir, PathSupport.toSafeName(filename));
    logger.debug("Attempting to write a file to {}", f.getAbsolutePath());
    FileOutputStream out = null;
    try {
      if (!f.exists()) {
        f.createNewFile();
      } else {
        logger.debug("Attempting to overwrite the file at {}", f.getAbsolutePath());
      }
      out = new FileOutputStream(f);
      IOUtils.copy(in, out);
      createMd5(f);
    } catch (IOException e) {
      FileUtils.deleteDirectory(dir);
      throw e;
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }
    return getURI(mediaPackageID, mediaPackageElementID, filename);
  }

  /**
   * Creates a file containing the md5 hash for the contents of a source file.
   * 
   * @param f
   *          the source file containing the data to hash
   * @throws IOException
   *           if the hash cannot be created
   */
  protected File createMd5(File f) throws IOException {
    FileInputStream md5In = null;
    File md5File = null;
    try {
      md5In = new FileInputStream(f);
      String md5 = DigestUtils.md5Hex(md5In);
      IOUtils.closeQuietly(md5In);
      md5File = getMd5File(f);
      FileUtils.writeStringToFile(md5File, md5);
      return md5File;
    } catch (IOException e) {
      FileUtils.deleteQuietly(md5File);
      throw e;
    } finally {
      IOUtils.closeQuietly(md5In);
    }
  }

  /**
   * Gets the file handle for an md5 associated with a content file. Calling this method and obtaining a File handle is
   * not a guarantee that the md5 file exists.
   * 
   * @param f
   *          The source file
   * @return The md5 file
   */
  private File getMd5File(File f) {
    return new File(f.getParent(), f.getName() + MD5_EXTENSION);
  }

  /**
   * Gets the file handle for a source file from its md5 file.
   * 
   * @param md5File
   *          The md5 file
   * @return The source file
   */
  protected File getSourceFile(File md5File) {
    return new File(md5File.getParent(), md5File.getName().substring(0, md5File.getName().length() - 4));
  }

  protected void checkPathSafe(String id) {
    if (id == null)
      throw new NullPointerException("IDs can not be null");
    if (id.indexOf("..") > -1 || id.indexOf(File.separator) > -1) {
      throw new IllegalArgumentException("Invalid media package, element ID, or file name");
    }
  }

  /**
   * Returns the file to the media package element.
   * 
   * @param mediaPackageID
   *          the media package identifier
   * @param mediaPackageElementID
   *          the media package element identifier
   * @return the file or <code>null</code> if no such element exists
   * @throws IllegalStateException
   *           if more than one matching elements were found
   */
  private File getFile(String mediaPackageID, String mediaPackageElementID) throws IllegalStateException {
    File directory = getElementDirectory(mediaPackageID, mediaPackageElementID);

    File[] md5Files = directory.listFiles(MD5_FINAME_FILTER);
    if (md5Files == null) {
      logger.debug("Element directory {} does not exist", directory);
      return null;
    } else if (md5Files.length == 0) {
      logger.debug("There are no complete files in the element directory {}", directory.getAbsolutePath());
      return null;
    } else if (md5Files.length == 1) {
      File f = getSourceFile(md5Files[0]);
      if (f.exists())
        return f;
      else
        return null;
    } else {
      logger.error("Integrity error: Element directory {} contains more than one element", mediaPackageID + "/"
              + mediaPackageElementID);
      throw new IllegalStateException("Directory " + mediaPackageID + "/" + mediaPackageElementID
              + "does not contain exactly one element");
    }
  }

  /**
   * Returns the file from the given collection.
   * 
   * @param collectionId
   *          the collection identifier
   * @param fileName
   *          the file name
   * @return the file
   * @throws NotFoundException
   *           if either the collection or the file don't exist
   */
  private File getFileFromCollection(String collectionId, String fileName) throws NotFoundException {
    File directory = null;
    try {
      directory = getCollectionDirectory(collectionId, false);
      if (directory == null)
        throw new NotFoundException(fileName);
    } catch (IOException e) {
      // can be ignored, since we don't want the directory to be created, so it will never happen
    }
    File sourceFile = new File(directory, PathSupport.toSafeName(fileName));
    File md5File = getMd5File(sourceFile);
    if (!sourceFile.exists() || !md5File.exists()) {
      throw new NotFoundException(fileName);
    }
    return sourceFile;
  }

  private File getElementDirectory(String mediaPackageID, String mediaPackageElementID) {
    return new File(PathSupport.concat(new String[] { rootDirectory, MEDIAPACKAGE_PATH_PREFIX, mediaPackageID,
            mediaPackageElementID }));
  }

  /**
   * Returns a <code>File</code> reference to collection. If the collection does not exist, the method either returns
   * <code>null</code> or creates it, depending on the parameter <code>create</code>.
   * 
   * @param collectionId
   *          the collection identifier
   * @param create
   *          whether to create a collection directory if it does not exist
   * @return the collection directory or <code>null</code> if it does not exist and should not be created
   * @throws IOException
   *           if creating a non-existing directory fails
   */
  private File getCollectionDirectory(String collectionId, boolean create) throws IOException {
    File collectionDir = new File(
            PathSupport.concat(new String[] { rootDirectory, COLLECTION_PATH_PREFIX, collectionId }));
    if (!collectionDir.exists()) {
      if (!create)
        return null;
      try {
        FileUtils.forceMkdir(collectionDir);
        logger.debug("Created collection directory " + collectionId);
      } catch (IOException e) {
        throw new IllegalStateException("Can not create collection directory" + collectionDir);
      }
    }
    return collectionDir;
  }

  void createRootDirectory() throws IOException {
    File f = new File(rootDirectory);
    if (!f.exists())
      FileUtils.forceMkdir(f);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionSize(java.lang.String)
   */
  @Override
  public long getCollectionSize(String id) throws NotFoundException {
    File collectionDir = null;
    try {
      collectionDir = getCollectionDirectory(id, false);
      if (collectionDir == null || !collectionDir.canRead())
        throw new NotFoundException("Can not find collection " + id);
    } catch (IOException e) {
      // can be ignored, since we don't want the directory to be created, so it will never happen
    }
    File[] files = collectionDir.listFiles(MD5_FINAME_FILTER);
    if (files == null)
      throw new IllegalArgumentException("Collection " + id + " is not a directory");
    return files.length;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public InputStream getFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
    checkPathSafe(collectionId);
    File f = getFileFromCollection(collectionId, fileName);
    if (f == null || !f.isFile()) {
      throw new NotFoundException("Unable to locate " + f + " in the working file repository");
    }
    logger.debug("Attempting to read file {}", f.getAbsolutePath());
    return new FileInputStream(f);
  }

  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   *           if the hash can't be created
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
    checkPathSafe(collectionId);
    checkPathSafe(fileName);
    File f = new File(PathSupport.concat(new String[] { rootDirectory, COLLECTION_PATH_PREFIX, collectionId,
            PathSupport.toSafeName(fileName) }));
    logger.debug("Attempting to write a file to {}", f.getAbsolutePath());
    FileOutputStream out = null;
    try {
      if (!f.exists()) {
        logger.debug("Attempting to create a new file at {}", f.getAbsolutePath());
        File collectionDirectory = getCollectionDirectory(collectionId, true);
        if (!collectionDirectory.exists()) {
          logger.debug("Attempting to create a new directory at {}", collectionDirectory.getAbsolutePath());
          FileUtils.forceMkdir(collectionDirectory);
        }
        f.createNewFile();
      } else {
        logger.debug("Attempting to overwrite the file at {}", f.getAbsolutePath());
      }
      out = new FileOutputStream(f);
      IOUtils.copy(in, out);
      createMd5(f);
    } catch (IOException e) {
      FileUtils.deleteQuietly(f);
      throw e;
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }
    return getCollectionURI(collectionId, fileName);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#copyTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI copyTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement,
          String toFileName) throws NotFoundException, IOException {
    File source = getFileFromCollection(fromCollection, fromFileName);
    if (source == null)
      throw new IllegalArgumentException("Source file " + fromCollection + "/" + fromFileName + " does not exist");
    File destDir = getElementDirectory(toMediaPackage, toMediaPackageElement);
    if (!destDir.exists()) {
      // we needed to create the directory, but couldn't
      try {
        FileUtils.forceMkdir(destDir);
      } catch (IOException e) {
        throw new IllegalStateException("could not create mediapackage/element directory '" + destDir.getAbsolutePath()
                + "' : " + e);
      }
    }
    File destFile;
    try {
      destFile = new File(destDir, toFileName);
      FileSupport.link(source, destFile);
      createMd5(destFile);
    } catch (Exception e) {
      FileUtils.deleteDirectory(destDir);
    }
    return getURI(toMediaPackage, toMediaPackageElement, toFileName);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#moveTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement,
          String toFileName) throws NotFoundException, IOException {
    File source = getFileFromCollection(fromCollection, fromFileName);
    File sourceMd5 = getMd5File(source);
    File destDir = getElementDirectory(toMediaPackage, toMediaPackageElement);
    if (!destDir.exists()) {
      // we needed to create the directory, but couldn't
      try {
        FileUtils.forceMkdir(destDir);
      } catch (IOException e) {
        throw new IllegalStateException("could not create mediapackage/element directory '" + destDir.getAbsolutePath()
                + "' : " + e);
      }
    }

    File dest = getFile(toMediaPackage, toMediaPackageElement);
    if (dest == null) {
      dest = new File(getElementDirectory(toMediaPackage, toMediaPackageElement), toFileName);
    }

    try {
      FileUtils.moveFile(source, dest);
      if (!sourceMd5.delete())
        throw new IOException("Unable to delete " + sourceMd5.getAbsolutePath());
      createMd5(dest);
    } catch (IOException e) {
      FileUtils.deleteDirectory(destDir);
      throw new IllegalStateException("unable to copy file" + e);
    }
    return getURI(toMediaPackage, toMediaPackageElement, dest.getName());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#deleteFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public boolean deleteFromCollection(String collectionId, String fileName) throws IOException {
    File f = null;
    try {
      f = getFileFromCollection(collectionId, fileName);
    } catch (NotFoundException e) {
      logger.trace("File {}/{} does not exist", collectionId, fileName);
      return false;
    }
    File md5File = getMd5File(f);

    if (!f.isFile())
      throw new IllegalStateException(f + " is not a regular file");
    if (!md5File.isFile())
      throw new IllegalStateException(md5File + " is not a regular file");
    if (!md5File.delete())
      throw new IOException("MD5 hash " + md5File + " cannot be deleted");
    if (!f.delete())
      throw new IOException(f + " cannot be deleted");

    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) throws NotFoundException {
    File collectionDir = null;
    try {
      collectionDir = getCollectionDirectory(collectionId, false);
      if (collectionDir == null)
        throw new NotFoundException(collectionId);
    } catch (IOException e) {
      // We are not asking for the collection to be created, so this exception is never thrown
    }

    File[] files = collectionDir.listFiles(MD5_FINAME_FILTER);
    URI[] uris = new URI[files.length];
    for (int i = 0; i < files.length; i++) {
      try {
        uris[i] = new URI(serviceUrl + COLLECTION_PATH_PREFIX + collectionId + "/"
                + PathSupport.toSafeName(getSourceFile(files[i]).getName()));
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Invalid URI for " + files[i]);
      }
    }

    return uris;
  }

  /**
   * Returns the md5 hash value for the given mediapackage element.
   * 
   * @throws NotFoundException
   *           if the media package element does not exist
   */
  String getMediaPackageElementDigest(String mediaPackageID, String mediaPackageElementID) throws IOException,
          IllegalStateException, NotFoundException {
    File f = getFile(mediaPackageID, mediaPackageElementID);
    if (f == null)
      throw new NotFoundException(mediaPackageID + "/" + mediaPackageElementID);
    return getFileDigest(f);
  }

  /**
   * Returns the md5 hash value for the given collection element.
   * 
   * @throws NotFoundException
   *           if the collection element does not exist
   */
  String getCollectionElementDigest(String collectionId, String fileName) throws IOException, NotFoundException {
    return getFileDigest(getFileFromCollection(collectionId, fileName));
  }

  /**
   * Returns the md5 of a file
   * 
   * @param file
   *          the source file
   * @return the md5 hash
   */
  private String getFileDigest(File file) throws IOException {
    if (file == null)
      throw new IllegalArgumentException("File must not be null");
    if (!file.exists() || !file.isFile())
      throw new IllegalArgumentException("File " + file.getAbsolutePath() + " can not be read");

    // Check if there is a precalculated md5 hash
    File md5HashFile = getMd5File(file);
    if (file.exists()) {
      logger.trace("Reading precalculated hash for {} from {}", file, md5HashFile.getName());
      return FileUtils.readFileToString(md5HashFile, "utf-8");
    }

    // Calculate the md5 hash
    InputStream in = null;
    String md5 = null;
    try {
      in = new FileInputStream(file);
      md5 = DigestUtils.md5Hex(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Write the md5 hash to disk for later reference
    try {
      FileUtils.writeStringToFile(md5HashFile, md5, "utf-8");
    } catch (IOException e) {
      logger.warn("Error storing cached md5 checksum at {}", md5HashFile);
      throw e;
    }

    return md5;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getTotalSpace()
   */
  public long getTotalSpace() {
    File f = new File(rootDirectory);
    return f.getTotalSpace();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsableSpace()
   */
  public long getUsableSpace() {
    File f = new File(rootDirectory);
    return f.getUsableSpace();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getDiskSpace()
   */
  public String getDiskSpace() {
    int usable = Math.round(getUsableSpace() / 1024 / 1024 / 1024);
    int total = Math.round(getTotalSpace() / 1024 / 1024 / 1024);
    long percent = Math.round(100.0 * getUsableSpace() / (1 + getTotalSpace()));
    return "Usable space " + usable + " Gb out of " + total + " Gb (" + percent + "%)";
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.PathMappable#getPathPrefix()
   */
  @Override
  public String getPathPrefix() {
    return rootDirectory;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.PathMappable#getUrlPrefix()
   */
  @Override
  public String getUrlPrefix() {
    return serviceUrl.toString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getBaseUri()
   */
  @Override
  public URI getBaseUri() {
    return serviceUrl;
  }

  /**
   * Sets the remote service manager.
   * 
   * @param remoteServiceManager
   */
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

}
