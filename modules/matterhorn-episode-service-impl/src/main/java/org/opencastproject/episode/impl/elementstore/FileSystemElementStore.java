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
package org.opencastproject.episode.impl.elementstore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.StoragePath;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;

import static org.opencastproject.util.IoSupport.file;
import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

public class FileSystemElementStore implements ElementStore {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(FileSystemElementStore.class);

  /** Configuration key for the storage directory */
  public static final String CONFIG_STORAGE_DIR = "org.opencastproject.storage.dir";

  /** Configuration key for the archive root directory */
  public static final String CONFIG_ARCHIVE_ROOT_DIR = "org.opencastproject.episode.rootdir";

  /** The default archive directory name */
  private static final String DEFAULT_ARCHIVE_DIRECTORY = "archive";

  /** The root directory for storing files */
  private String rootDirectory = null;

  /** The http client */
  private TrustedHttpClient httpClient;

  /**
   * Sets the trusted http client
   * 
   * @param httpClient
   *          the http client
   */
  public void setHttpClient(TrustedHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Service activator, called via declarative services configuration.
   * 
   * @param cc
   *          the component context
   */
  public void activate(final ComponentContext cc) throws IllegalStateException, IOException {
    rootDirectory = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_ARCHIVE_ROOT_DIR));

    if (rootDirectory == null) {
      final String storageDir = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_STORAGE_DIR));
      if (storageDir == null)
        throw new IllegalArgumentException("Storage directory must be set");
      rootDirectory = PathSupport.concat(storageDir, DEFAULT_ARCHIVE_DIRECTORY);
    }

    try {
      File f = new File(rootDirectory);
      if (!f.exists())
        FileUtils.forceMkdir(f);
    } catch (IOException e) {
      logger.error("Unable to create the archive repository root directory at {}", rootDirectory);
      throw e;
    }
  }

  /**
   * @see org.opencastproject.episode.impl.elementstore.ElementStore#put(org.opencastproject.episode.impl.StoragePath, Source)
   */
  @Override
  public void put(StoragePath storagePath, Source source) throws ElementStoreException {
    InputStream in = null;
    FileOutputStream output = null;
    HttpResponse response = null;
    File destination = null;
    try {
      File destDir = getStoragePathDir(storagePath);
      if (!destDir.exists())
        FileUtils.forceMkdir(destDir);
      destination = new File(destDir, getFileName(storagePath, source.getUri()));

      HttpGet getRequest = new HttpGet(source.getUri());
      response = httpClient.execute(getRequest);
      in = response.getEntity().getContent();
      output = FileUtils.openOutputStream(destination);
      IOUtils.copy(in, output);
    } catch (Exception e) {
      FileUtils.deleteQuietly(destination);
      logger.error("Error storing source {} to archive {}", source, destination.getAbsolutePath());
      throw new ElementStoreException(e);
    } finally {
      IOUtils.closeQuietly(output);
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
  }

  /**
   * @see org.opencastproject.episode.impl.elementstore.ElementStore#copy(StoragePath, StoragePath)
   */
  @Override
  public boolean copy(StoragePath from, StoragePath to) throws ElementStoreException {
    Option<File> sourceFile = findStoragePathFile(from);
    if (sourceFile.isNone())
      return false;

    File destDir = null;
    try {
      destDir = getStoragePathDir(to);
      if (!destDir.exists())
        FileUtils.forceMkdir(destDir);

      FileSupport.link(sourceFile.get(), destDir, true);
      return true;
    } catch (IOException e) {
      logger.error("Error copying archive file {} to {}", sourceFile, destDir);
      throw new ElementStoreException(e);
    }
  }

  /**
   * @see org.opencastproject.episode.impl.elementstore.ElementStore#get(StoragePath)
   */
  @Override
  public Option<InputStream> get(final StoragePath path) throws ElementStoreException {
    Option<File> file = findStoragePathFile(path);
    if (file.isNone())
      return Option.<InputStream> none();

    try {
      return Option.<InputStream> some(new FileInputStream(file.get()));
    } catch (FileNotFoundException e) {
      logger.error("Error getting archiv file {}", file);
      throw new ElementStoreException(e);
    }
  }

  /**
   * @see org.opencastproject.episode.impl.elementstore.ElementStore#delete(DeletionSelector)
   */
  @Override
  public boolean delete(DeletionSelector sel) throws ElementStoreException {
    File dir = getDeletionSelectorDir(sel);
    try {
      FileUtils.deleteDirectory(dir);
      return true;
    } catch (IOException e) {
      logger.error("Error deleting directory from archive {}", dir);
      throw new ElementStoreException(e);
    }
  }

  /**
   * Returns a file name from a {@link StoragePath} and {@link URI}
   * 
   * @param storagePath
   *          the storage path
   * @param source
   *          the URI
   * @return the file name
   * @throws MalformedURLException
   *           If a protocol handler for the URL could not be found, or if some other error occurred while constructing
   *           the URL
   */
  private String getFileName(StoragePath storagePath, URI source) throws MalformedURLException {
    String extension = FilenameUtils.getExtension(source.toURL().getPath());
    if (StringUtils.isBlank(extension))
      return storagePath.getMediaPackageElementId();

    return storagePath.getMediaPackageElementId() + FilenameUtils.EXTENSION_SEPARATOR + extension;
  }

  /**
   * Returns the directory file from a deletion selector
   * 
   * @param sel
   *          the deletion selector
   * @return the directory file
   */
  private File getDeletionSelectorDir(DeletionSelector sel) {
    final String basePath = path(rootDirectory, sel.getOrganizationId(), sel.getMediaPackageId());
    for (Version v : sel.getVersion())
      return file(basePath, v.toString());
    return file(basePath);
  }

  /**
   * Returns the storage path directory file
   * 
   * @param storagePath
   *          the storage path
   * @return the directory file
   */
  private File getStoragePathDir(StoragePath storagePath) {
    return file(rootDirectory, storagePath.getOrganizationId(),
                storagePath.getMediaPackageId(), storagePath.getVersion().toString());
  }

  /**
   * Returns a file {@link Option} from a storage path if one is found or an empty {@link Option}
   * 
   * @param storagePath
   *          the storage path
   * @return the file {@link Option}
   */
  private Option<File> findStoragePathFile(final StoragePath storagePath) {
    final FilenameFilter filter = new FilenameFilter() {
      @Override public boolean accept(File dir, String name) {
        return FilenameUtils.getBaseName(name).equals(storagePath.getMediaPackageElementId());
      }
    };
    return option(getStoragePathDir(storagePath).listFiles(filter)).bind(new Function<File[], Option<File>>() {
      @Override public Option<File> apply(File[] files) {
        switch (files.length) {
          case 0:
            return none();
          case 1:
            return some(files[0]);
          default:
            throw new ElementStoreException("Storage path " + files[0].getParent() + "contains multiple files whit the same element id!: "
                                                    + storagePath.getMediaPackageElementId());
        }
      }
    });
  }

}
