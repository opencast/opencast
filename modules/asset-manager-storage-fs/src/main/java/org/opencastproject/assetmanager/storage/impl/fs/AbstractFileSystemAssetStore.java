/*
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
package org.opencastproject.assetmanager.storage.impl.fs;

import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.opencastproject.util.FileSupport.link;
import static org.opencastproject.util.IoSupport.file;
import static org.opencastproject.util.data.functions.Strings.trimToNone;

import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class AbstractFileSystemAssetStore implements AssetStore {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractFileSystemAssetStore.class);

  /** The store type e.g. filesystem (short-term), aws (long-term), other implementations */
  protected String storeType = null;

  protected abstract Workspace getWorkspace();

  protected abstract String getRootDirectory();
  protected abstract String getRootDirectory(String orgId, String mpId);

  /**
   * Optional further handling of the complete deletion of mediapackage from the local store.
   * This method will be called after the deletion of the mediapackage directory.
   * @param orgId Organization ID
   * @param mpId Mediapackage ID
   */
  protected abstract void onDeleteMediaPackage(String orgId, String mpId);

  @Override
  public void put(StoragePath storagePath, Source source) throws AssetStoreException {
    // Retrieving the file from the workspace has the advantage that in most cases the file already exists in the local
    // working file repository. In the very few cases where the file is not in the working file repository,
    // this strategy leads to a minor overhead because the file not only gets downloaded and stored in the file system
    // but also a hard link needs to be created (or if that's not possible, a copy of the file.
    final File origin = getUniqueFileFromWorkspace(source);
    final File destination = createFile(storagePath, source);
    try {
      mkParent(destination);
      link(origin, destination);
    } catch (IOException e) {
      logger.error("Error while linking/copying file {} to {}: {}", origin, destination, getMessage(e));
      throw new AssetStoreException(e);
    } finally {
      if (origin != null) {
        FileUtils.deleteQuietly(origin);
      }
    }
  }

  private File getUniqueFileFromWorkspace(Source source) {
    try {
      return getWorkspace().get(source.getUri(), true);
    } catch (NotFoundException e) {
      logger.error("Source file '{}' does not exist", source.getUri());
      throw new AssetStoreException(e);
    } catch (IOException e) {
      logger.error("Error while getting file '{}' from workspace: {}", source.getUri(), getMessage(e));
      throw new AssetStoreException(e);
    }
  }

  @Override
  public boolean copy(final StoragePath from, final StoragePath to) throws AssetStoreException {
    var file = findStoragePathFile(from);
    if (file.isPresent()) {
      var f = file.get();

      final File t = createFile(to, f);
      mkParent(t);
      logger.debug("Copying {} to {}", f.getAbsolutePath(), t.getAbsolutePath());
      try {
        link(f, t, true);
      } catch (IOException e) {
        logger.error("Error copying archive file {} to {}", f, t);
        throw new AssetStoreException(e);
      }
      return true;
    }
    return false;
  }

  @Override
  public Optional<InputStream> get(final StoragePath path) throws AssetStoreException {
    var file = findStoragePathFile(path);
    if (file.isPresent()) {
      try {
        return Optional.of(new FileInputStream(file.get()));
      } catch (FileNotFoundException e) {
        logger.error("Error getting archive file {}", file);
        throw new AssetStoreException(e);
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean contains(StoragePath path) throws AssetStoreException {
    return findStoragePathFile(path).isPresent();
  }

  @Override
  public boolean delete(DeletionSelector sel) throws AssetStoreException {
    // Resolve the root directory once, before anything is deleted. Deleting the directory below may remove the very
    // directory a second lookup would search for, so the result must not be resolved again afterwards.
    final String rootPath = getRootDirectory(sel.getOrganizationId(), sel.getMediaPackageId());
    if (rootPath == null) {
      // MediaPackage could not be found locally. This could mean
      //   - all snapshots live in a remote asset store
      //   - mount failed and files are temporary not available
      //   - file was deleted out-of-band
      //   - other fs problem
      // In any case, we cannot continue and return "false" to indicate that the files could not be found.
      return false;
    }
    final File dir = getDeletionSelectorDir(rootPath, sel);
    try {
      FileUtils.deleteDirectory(dir);
      // also delete the media package directory if all versions have been deleted
      boolean mpDirDeleted = FileSupport.deleteHierarchyIfEmpty(
              file(rootPath, sel.getOrganizationId()), dir.getParentFile());
      if (mpDirDeleted) {
        onDeleteMediaPackage(sel.getOrganizationId(), sel.getMediaPackageId());
      }
      return true;
    } catch (IOException e) {
      throw new AssetStoreException("Error deleting directory from archive " + dir, e);
    }
  }

  /**
   * Returns the directory file from a deletion selector
   *
   * @param rootPath
   *          the root directory of the media package the selector refers to
   * @param sel
   *          the deletion selector
   * @return the directory file
   */
  private File getDeletionSelectorDir(String rootPath, DeletionSelector sel) {
    final String basePath = Paths.get(rootPath, sel.getOrganizationId(), sel.getMediaPackageId()).toString();
    if (sel.getVersion().isPresent()) {
      return file(basePath, sel.getVersion().get().toString());
    }
    return file(basePath);
  }

  /** Create all parent directories of a file. */
  private void mkParent(File f) {
    mkDirs(f.getParentFile());
  }

  /** Create this directory and all of its parents. */
  protected void mkDirs(File d) {
    if (d == null) {
      return;
    }
    // mkdirs *may* not have succeeded if it returns false, so check if the directory exists in that case
    if (!d.mkdirs() && !d.exists()) {
      final String msg = "Cannot create directory " + d;
      logger.error(msg);
      throw new AssetStoreException(msg);
    }
  }

  /** Return the extension of a file. */
  private Optional<String> extension(File f) {
    Optional<String> opt = trimToNone(getExtension(f.getAbsolutePath()));
    return opt.isPresent()
        ? Optional.of(opt.get())
        : Optional.empty();
  }

  /** Return the extension of a URI, i.e. the extension of its path. */
  private Optional<String> extension(URI uri) {
    try {
      Optional<String> opt = trimToNone(getExtension(uri.toURL().getPath()));
      return opt.isPresent()
          ? Optional.of(opt.get())
          : Optional.empty();
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
  }

  /** Create a file from a storage path and the extension of file <code>f</code>. */
  private File createFile(StoragePath p, File f) {
    return createFile(p, extension(f));
  }

  /** Create a file from a storage path and the extension of the URI of <code>s</code>. */
  private File createFile(StoragePath p, Source s) {
    return createFile(p, extension(s.getUri()));
  }

  /** Create a file from a storage path and an optional extension. */
  private File createFile(StoragePath p, Optional<String> extension) {
    String rootDirectory = getRootDirectory(p.getOrganizationId(), p.getMediaPackageId());
    if (rootDirectory == null) {
      rootDirectory = getRootDirectory();
    }
    return file(
            rootDirectory,
            p.getOrganizationId(),
            p.getMediaPackageId(),
            p.getVersion().toString(),
            extension.isPresent() ? p.getMediaPackageElementId() + EXTENSION_SEPARATOR + extension.get() : p
                    .getMediaPackageElementId());
  }

  /** Returns a file from a storage path if it exists, null otherwise */
  private File getExistingFile(StoragePath p, Optional<String> extension) {
    String rootDirectory = getRootDirectory(p.getOrganizationId(), p.getMediaPackageId());
    if (rootDirectory == null) {
      return null;
    }
    return file(
            rootDirectory,
            p.getOrganizationId(),
            p.getMediaPackageId(),
            p.getVersion().toString(),
            extension.isPresent() ? p.getMediaPackageElementId() + EXTENSION_SEPARATOR + extension.get() : p
                    .getMediaPackageElementId());
  }

  /**
   * Returns a file {@link Optional} from a storage path if one is found or an empty {@link Optional}
   *
   * @param storagePath
   *          the storage path
   * @return the file {@link Optional}
   */
  private Optional<File> findStoragePathFile(final StoragePath storagePath) {
    final FilenameFilter filter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return FilenameUtils.getBaseName(name).equals(storagePath.getMediaPackageElementId());
      }
    };
    final File containerDir = getExistingFile(storagePath, Optional.empty()).getParentFile();

    var files = containerDir.listFiles(filter);
    if (files == null) {
      return Optional.empty();
    }
    switch (files.length) {
      case 0:
        return Optional.empty();
      case 1:
        return Optional.of(files[0]);
      default:
        throw new AssetStoreException("Storage path " + files[0].getParent()
            + "contains multiple files with the same element id!: " + storagePath.getMediaPackageElementId());
    }
  }

  @Override
  public String getStoreType() {
    return storeType;
  }

}
