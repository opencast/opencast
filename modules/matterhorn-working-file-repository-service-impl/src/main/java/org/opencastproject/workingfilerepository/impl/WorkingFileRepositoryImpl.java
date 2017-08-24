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

package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.workingfilerepository.api.PathMappable;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workingfilerepository.jmx.WorkingFileRepositoryBean;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

import javax.management.ObjectInstance;

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory using the media package
 * ID as a subdirectory and the media package element ID as the file name.
 */
public class WorkingFileRepositoryImpl implements WorkingFileRepository, PathMappable {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl.class);
  private static final Log log = new Log(logger);

  /** The extension we use for the md5 hash calculated from the file contents */
  public static final String MD5_EXTENSION = ".md5";

  /** The filename filter matching .md5 files */
  private static final FilenameFilter MD5_FINAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return name.endsWith(MD5_EXTENSION);
    }
  };

  /** Working file repository JMX type */
  private static final String JMX_WORKING_FILE_REPOSITORY_TYPE = "WorkingFileRepository";

  /** The JMX working file repository bean */
  private WorkingFileRepositoryBean workingFileRepositoryBean = new WorkingFileRepositoryBean(this);

  /** The JMX bean object instance */
  private ObjectInstance registeredMXBean;

  /** The remote service manager */
  protected ServiceRegistry remoteServiceManager;

  /** The root directory for storing files */
  protected String rootDirectory = null;

  /** The Base URL for this server */
  protected String serverUrl = null;

  /** The URL path for the services provided by the working file repository */
  protected String servicePath = null;

  /** The security service to get current organization from */
  protected SecurityService securityService;

  /**
   * Activate the component
   */
  public void activate(ComponentContext cc) throws IOException {
    if (rootDirectory != null)
      return; // If the root directory was set, respect that setting

    // server url
    serverUrl = cc.getBundleContext().getProperty(MatterhornConstants.SERVER_URL_PROPERTY);
    if (StringUtils.isBlank(serverUrl))
      throw new IllegalStateException("Server URL must be set");

    // working file repository 'facade' configuration
    servicePath = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);

    // root directory
    rootDirectory = StringUtils.trimToNull(cc.getBundleContext().getProperty("org.opencastproject.file.repo.path"));
    if (rootDirectory == null) {
      String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
      if (storageDir == null) {
        throw new IllegalStateException("Storage directory must be set");
      }
      rootDirectory = storageDir + File.separator + "files";
    }

    try {
      createRootDirectory();
    } catch (IOException e) {
      logger.error("Unable to create the working file repository root directory at {}", rootDirectory);
      throw e;
    }

    registeredMXBean = JmxUtil.registerMXBean(workingFileRepositoryBean, JMX_WORKING_FILE_REPOSITORY_TYPE);

    logger.info(getDiskSpace());
  }

  /**
   * Callback from OSGi on service deactivation.
   */
  public void deactivate() {
    JmxUtil.unregisterMXBean(registeredMXBean);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#delete(java.lang.String, java.lang.String)
   */
  public boolean delete(String mediaPackageID, String mediaPackageElementID) throws IOException {
    File f;
    try {
      f = getFile(mediaPackageID, mediaPackageElementID);

      File parentDirectory = f.getParentFile();
      logger.debug("Attempting to delete {}", parentDirectory.getAbsolutePath());
      FileUtils.forceDelete(parentDirectory);
      File parentsParentDirectory = parentDirectory.getParentFile();
      if (parentsParentDirectory.isDirectory() && parentsParentDirectory.list().length == 0)
        FileUtils.forceDelete(parentDirectory.getParentFile());
      return true;
    } catch (NotFoundException e) {
      log.info("Unable to delete non existing media package element {}@{}", mediaPackageElementID, mediaPackageID);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#get(java.lang.String, java.lang.String)
   */
  public InputStream get(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
    File f = getFile(mediaPackageID, mediaPackageElementID);
    logger.debug("Attempting to read file {}", f.getAbsolutePath());
    return new FileInputStream(f);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionURI(java.lang.String,
   * java.lang.String)
   */
  @Override
  public URI getCollectionURI(String collectionID, String fileName) {
    try {
      return new URI(getBaseUri() + COLLECTION_PATH_PREFIX + collectionID + "/" + PathSupport.toSafeName(fileName));
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
   * java.lang.String)
   */
  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID, String fileName) {
    String uri = UrlSupport.concat(new String[]{getBaseUri().toString(), MEDIAPACKAGE_PATH_PREFIX, mediaPackageID,
            mediaPackageElementID});
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
   * java.lang.String, java.io.InputStream)
   */
  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in)
          throws IOException {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File dir = getElementDirectory(mediaPackageID, mediaPackageElementID);

    File[] filesToDelete = null;

    if (dir.exists()) {
      filesToDelete = dir.listFiles();
    } else {
      logger.debug("Attempting to create a new directory at {}", dir.getAbsolutePath());
      FileUtils.forceMkdir(dir);
    }

    // Destination files
    File f = new File(dir, PathSupport.toSafeName(filename));
    File md5File = getMd5File(f);

    // Temporary files while adding
    File fTmp = null;
    File md5FileTmp = null;

    if (f.exists()) {
      logger.debug("Updating file {}", f.getAbsolutePath());
    } else {
      logger.debug("Adding file {}", f.getAbsolutePath());
    }

    FileOutputStream out = null;
    try {

      fTmp = File.createTempFile(f.getName(), ".tmp", dir);
      md5FileTmp = File.createTempFile(md5File.getName(), ".tmp", dir);

      logger.trace("Writing to new temporary file {}", fTmp.getAbsolutePath());

      out = new FileOutputStream(fTmp);

      // Wrap the input stream and copy the input stream to the file
      MessageDigest messageDigest = null;
      DigestInputStream dis = null;
      try {
        messageDigest = MessageDigest.getInstance("MD5");
        dis = new DigestInputStream(in, messageDigest);
        IOUtils.copy(dis, out);
      } catch (NoSuchAlgorithmException e1) {
        logger.error("Unable to create md5 message digest");
      }

      // Store the hash
      String md5 = Checksum.convertToHex(dis.getMessageDigest().digest());
      try {
        FileUtils.writeStringToFile(md5FileTmp, md5);
      } catch (IOException e) {
        FileUtils.deleteQuietly(md5FileTmp);
        throw e;
      } finally {
        IOUtils.closeQuietly(dis);
      }

    } catch (IOException e) {
      FileUtils.deleteDirectory(dir);
      throw e;
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }

    // Rename temporary files to the final version atomically
    try {
      Files.move(md5FileTmp.toPath(), md5File.toPath(), StandardCopyOption.ATOMIC_MOVE);
      Files.move(fTmp.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      logger.trace("Atomic move not supported by this filesystem: using replace instead");
      Files.move(md5FileTmp.toPath(), md5File.toPath(), StandardCopyOption.REPLACE_EXISTING);
      Files.move(fTmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Clean up any other files
    if (filesToDelete != null && filesToDelete.length > 0) {
      for (File fileToDelete : filesToDelete) {
        if (!fileToDelete.equals(f) && !fileToDelete.equals(md5File)) {
          logger.trace("delete {}", fileToDelete.getAbsolutePath());
          if (!fileToDelete.delete()) {
            throw new IllegalStateException("Unable to delete file: " + fileToDelete.getAbsolutePath());
          }
        }
      }
    }

    return getURI(mediaPackageID, mediaPackageElementID, filename);
  }

  /**
   * Creates a file containing the md5 hash for the contents of a source file.
   *
   * @param f
   *         the source file containing the data to hash
   * @throws IOException
   *         if the hash cannot be created
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
   * Creates a file containing the md5 hash for the contents of a source file.
   *
   * @param is
   *         the input stream containing the data to hash
   * @throws IOException
   *         if the hash cannot be created
   */
  protected String createMd5(InputStream is) throws IOException {
    File md5File = null;
    try {
      return DigestUtils.md5Hex(is);
    } catch (IOException e) {
      FileUtils.deleteQuietly(md5File);
      throw e;
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Gets the file handle for an md5 associated with a content file. Calling this method and obtaining a File handle is
   * not a guarantee that the md5 file exists.
   *
   * @param f
   *         The source file
   * @return The md5 file
   */
  private File getMd5File(File f) {
    return new File(f.getParent(), f.getName() + MD5_EXTENSION);
  }

  /**
   * Gets the file handle for a source file from its md5 file.
   *
   * @param md5File
   *         The md5 file
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
   *         the media package identifier
   * @param mediaPackageElementID
   *         the media package element identifier
   * @return the file or <code>null</code> if no such element exists
   * @throws IllegalStateException
   *         if more than one matching elements were found
   * @throws NotFoundException
   *         if the file cannot be found in the Working File Repository
   */
  protected File getFile(String mediaPackageID, String mediaPackageElementID) throws IllegalStateException,
          NotFoundException {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File directory = getElementDirectory(mediaPackageID, mediaPackageElementID);

    File[] md5Files = directory.listFiles(MD5_FINAME_FILTER);
    if (md5Files == null) {
      logger.debug("Element directory {} does not exist", directory);
      throw new NotFoundException("Element directory " + directory + " does not exist");
    } else if (md5Files.length == 0) {
      logger.debug("There are no complete files in the element directory {}", directory.getAbsolutePath());
      throw new NotFoundException("There are no complete files in the element directory " + directory.getAbsolutePath());
    } else if (md5Files.length == 1) {
      File f = getSourceFile(md5Files[0]);
      if (f.exists())
        return f;
      else
        throw new NotFoundException("Unable to locate " + f + " in the working file repository");
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
   *         the collection identifier
   * @param fileName
   *         the file name
   * @return the file
   * @throws NotFoundException
   *         if either the collection or the file don't exist
   */
  protected File getFileFromCollection(String collectionId, String fileName) throws NotFoundException,
          IllegalArgumentException {
    checkPathSafe(collectionId);

    File directory = null;
    try {
      directory = getCollectionDirectory(collectionId, false);
      if (directory == null) {
        //getCollectionDirectory returns null on a non-existant directory which is not being created...
        directory = new File(PathSupport.concat(new String[] { rootDirectory, COLLECTION_PATH_PREFIX, collectionId }));
        throw new NotFoundException(directory.getAbsolutePath());
      }
    } catch (IOException e) {
      // can be ignored, since we don't want the directory to be created, so it will never happen
    }
    File sourceFile = new File(directory, PathSupport.toSafeName(fileName));
    File md5File = getMd5File(sourceFile);
    if (!sourceFile.exists())
      throw new NotFoundException(sourceFile.getAbsolutePath());
    if (!md5File.exists())
      throw new NotFoundException(md5File.getAbsolutePath());
    return sourceFile;
  }

  private File getElementDirectory(String mediaPackageID, String mediaPackageElementID) {
    return new File(PathSupport.concat(new String[]{rootDirectory, MEDIAPACKAGE_PATH_PREFIX, mediaPackageID,
            mediaPackageElementID}));
  }

  /**
   * Returns a <code>File</code> reference to collection. If the collection does not exist, the method either returns
   * <code>null</code> or creates it, depending on the parameter <code>create</code>.
   *
   * @param collectionId
   *         the collection identifier
   * @param create
   *         whether to create a collection directory if it does not exist
   * @return the collection directory or <code>null</code> if it does not exist and should not be created
   * @throws IOException
   *         if creating a non-existing directory fails
   */
  private File getCollectionDirectory(String collectionId, boolean create) throws IOException {
    File collectionDir = new File(
            PathSupport.concat(new String[]{rootDirectory, COLLECTION_PATH_PREFIX, collectionId}));
    if (!collectionDir.exists()) {
      if (!create)
        return null;
      try {
        FileUtils.forceMkdir(collectionDir);
        logger.debug("Created collection directory " + collectionId);
      } catch (IOException e) {
        // We check again to see if it already exists because this collection dir may live on a shared disk.
        // Synchronizing does not help because the other instance is not in the same JVM.
        if (!collectionDir.exists()) {
          throw new IllegalStateException("Can not create collection directory" + collectionDir);
        }
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
   * java.lang.String)
   */
  @Override
  public InputStream getFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
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
   *         if the hash can't be created
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String,
   * java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
    checkPathSafe(collectionId);
    checkPathSafe(fileName);
    File f = new File(PathSupport.concat(new String[]{rootDirectory, COLLECTION_PATH_PREFIX, collectionId,
            PathSupport.toSafeName(fileName)}));
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

      // Wrap the input stream and copy the input stream to the file
      MessageDigest messageDigest = null;
      DigestInputStream dis = null;
      try {
        messageDigest = MessageDigest.getInstance("MD5");
        dis = new DigestInputStream(in, messageDigest);
        IOUtils.copy(dis, out);
      } catch (NoSuchAlgorithmException e1) {
        logger.error("Unable to create md5 message digest");
      }

      // Store the hash
      String md5 = Checksum.convertToHex(dis.getMessageDigest().digest());
      File md5File = null;
      try {
        md5File = getMd5File(f);
        FileUtils.writeStringToFile(md5File, md5);
      } catch (IOException e) {
        FileUtils.deleteQuietly(md5File);
        throw e;
      } finally {
        IOUtils.closeQuietly(dis);
      }

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
   * java.lang.String, java.lang.String, java.lang.String)
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
      destFile = new File(destDir, PathSupport.toSafeName(toFileName));
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
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement,
                    String toFileName) throws NotFoundException, IOException {
    File source = getFileFromCollection(fromCollection, fromFileName);
    File sourceMd5 = getMd5File(source);
    File destDir = getElementDirectory(toMediaPackage, toMediaPackageElement);

    logger.debug("Moving {} from {} to {}/{}", new String[]{fromFileName, fromCollection, toMediaPackage,
            toMediaPackageElement});
    if (!destDir.exists()) {
      // we needed to create the directory, but couldn't
      try {
        FileUtils.forceMkdir(destDir);
      } catch (IOException e) {
        throw new IllegalStateException("could not create mediapackage/element directory '" + destDir.getAbsolutePath()
                                                + "' : " + e);
      }
    }

    File dest = null;
    try {
      dest = getFile(toMediaPackage, toMediaPackageElement);
      logger.debug("Removing existing file from target location at {}", dest);
      delete(toMediaPackage, toMediaPackageElement);
    } catch (NotFoundException e) {
      dest = new File(getElementDirectory(toMediaPackage, toMediaPackageElement), PathSupport.toSafeName(toFileName));
    }

    try {
      FileUtils.moveFile(source, dest);
      FileUtils.moveFile(sourceMd5, getMd5File(dest));
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
   * java.lang.String)
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

    File parentDirectory = f.getParentFile();
    if (parentDirectory.isDirectory() && parentDirectory.list().length == 0) {
      logger.debug("Attempting to delete empty collection directory {}", parentDirectory.getAbsolutePath());
      try {
        FileUtils.forceDelete(parentDirectory);
      } catch (IOException e) {
        logger.warn("Unable to delete empty collection directory {}", parentDirectory.getAbsolutePath());
        return false;
      }
    }
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
        uris[i] = new URI(getBaseUri() + COLLECTION_PATH_PREFIX + collectionId + "/"
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
   *         if the media package element does not exist
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
   *         if the collection element does not exist
   */
  String getCollectionElementDigest(String collectionId, String fileName) throws IOException, NotFoundException {
    return getFileDigest(getFileFromCollection(collectionId, fileName));
  }

  /**
   * Returns the md5 of a file
   *
   * @param file
   *         the source file
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
  public Option<Long> getTotalSpace() {
    File f = new File(rootDirectory);
    return Option.some(f.getTotalSpace());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsableSpace()
   */
  public Option<Long> getUsableSpace() {
    File f = new File(rootDirectory);
    return Option.some(f.getUsableSpace());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsedSpace()
   */
  @Override
  public Option<Long> getUsedSpace() {
    return Option.some(FileUtils.sizeOfDirectory(new File(rootDirectory)));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getDiskSpace()
   */
  public String getDiskSpace() {
    int usable = Math.round(getUsableSpace().get() / 1024 / 1024 / 1024);
    int total = Math.round(getTotalSpace().get() / 1024 / 1024 / 1024);
    long percent = Math.round(100.0 * getUsableSpace().get() / (1 + getTotalSpace().get()));
    return "Usable space " + usable + " Gb out of " + total + " Gb (" + percent + "%)";
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#cleanupOldFilesFromCollection
   */
  @Override
  public boolean cleanupOldFilesFromCollection(String collectionId, long days) throws IOException {
    File colDir = getCollectionDirectory(collectionId, false);
    // Collection doesn't exist?
    if (colDir == null) {
      logger.trace("Collection {} does not exist", collectionId);
      return false;
    }

    logger.info("Cleaning up files older than {} days from collection {}", days, collectionId);

    if (!colDir.isDirectory())
      throw new IllegalStateException(colDir + " is not a directory");

    long referenceTime = System.currentTimeMillis() - days * 24 * 3600 * 1000;
    for (File f : colDir.listFiles()) {
      long lastModified = f.lastModified();
      logger.trace("{} last modified: {}, reference date: {}",
              new Object[] { f.getName(), new Date(lastModified), new Date(referenceTime) });
      if (lastModified <= referenceTime) {
        // Delete file
        deleteFromCollection(collectionId, f.getName());
        logger.info("Cleaned up file {} from collection {}", f.getName(), collectionId);
      }
    }

    return true;
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
    return getBaseUri().toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getBaseUri()
   */
  @Override
  public URI getBaseUri() {
    if (securityService.getOrganization() != null) {
      Map<String, String> orgProps = securityService.getOrganization().getProperties();
      if (orgProps != null && orgProps.containsKey(MatterhornConstants.WFR_URL_ORG_PROPERTY)) {
        try {
          return new URI(UrlSupport.concat(orgProps.get(MatterhornConstants.WFR_URL_ORG_PROPERTY), servicePath));
        } catch (URISyntaxException ex) {
          logger.warn("Organization working file repository URL not set, fallback to server URL");
        }
      }
    }

    return URI.create(UrlSupport.concat(serverUrl, servicePath));
  }

  /**
   * Sets the remote service manager.
   *
   * @param remoteServiceManager
   */
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
