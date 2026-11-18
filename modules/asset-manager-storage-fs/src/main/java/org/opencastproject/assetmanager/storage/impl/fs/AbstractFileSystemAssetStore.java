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

import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.util.AssetStoreDirectory;
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
import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractFileSystemAssetStore implements AssetStore {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractFileSystemAssetStore.class);

  /** The store type e.g. filesystem (short-term), aws (long-term), other implementations */
  protected String storeType = null;

  protected abstract Workspace getWorkspace();

  /** The root directory to store new media packages in */
  protected abstract AssetStoreDirectory getRootDirectory();

  /** The root directory containing the given media package, or null if it is not stored locally */
  protected abstract AssetStoreDirectory getRootDirectory(String orgId, String mpId);

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
    // Reject unsafe identifiers before they are used for anything, including the lookup of
    // the root directory, which joins them into a path as well
    ensureDeletionSelectorComponents(sel);
    // Resolve the root directory once, before anything is deleted. Deleting the directory below may remove the very
    // directory a second lookup would search for, so the result must not be resolved again afterwards.
    final AssetStoreDirectory rootDirectory = getRootDirectory(sel.getOrganizationId(), sel.getMediaPackageId());
    if (rootDirectory == null) {
      // MediaPackage could not be found locally. This could mean
      //   - all snapshots live in a remote asset store
      //   - mount failed and files are temporary not available
      //   - file was deleted out-of-band
      //   - other fs problem
      // In any case, we cannot continue and return "false" to indicate that the files could not be found.
      return false;
    }
    final File dir = getDeletionSelectorDir(rootDirectory, sel);
    // The deletion target must be exactly the media package directory (or one of its version
    // sub-directories) below the store root — never a parent of it. The organization and
    // media package identifiers are joined into the path, so an identifier that is the
    // current or parent directory reference ('.' or '..') or the empty string would collapse
    // the path into the parent directory; the check verifies the exact path shape instead of
    // relying on lexical containment alone.
    ensureMediaPackageShape(dir, rootDirectory, sel.getOrganizationId(), sel.getMediaPackageId(),
            sel.getVersion());
    try {
      FileUtils.deleteDirectory(dir);
      // also delete the media package directory if all versions have been deleted. Prefix directories are kept.
      boolean mpDirDeleted = FileSupport.deleteHierarchyIfEmpty(
              rootDirectory.deletionLimit(sel.getOrganizationId(), sel.getMediaPackageId()).toFile(),
              dir.getParentFile());
      if (mpDirDeleted) {
        onDeleteMediaPackage(sel.getOrganizationId(), sel.getMediaPackageId());
      }
      return true;
    } catch (IOException e) {
      throw new AssetStoreException("Error deleting directory from archive " + dir, e);
    }
  }

  /**
   * Ensures the given file lies within the given root directory (see
   * {@link FileSupport#ensureWithinRoot(File, File)}).
   *
   * @param target
   *          the file to check
   * @param root
   *          the root directory the file must be contained in
   * @throws AssetStoreException
   *         if the file is not within the root directory
   */
  private void ensureWithinRoot(File target, String root) throws AssetStoreException {
    try {
      FileSupport.ensureWithinRoot(target, new File(root));
    } catch (IllegalArgumentException e) {
      throw new AssetStoreException("Path is outside the asset store root " + new File(root).getAbsolutePath(), e);
    }
  }

  /**
   * Ensures the given directory is exactly the media package directory below the given store
   * root, or — if a version is present — one of its version sub-directories, and nothing else.
   * The organization and media package identifiers are joined into the path, so an identifier
   * that is the current or parent directory reference ({@code .} or {@code ..}) or the empty
   * string would collapse the path into a parent (or sibling) directory. Such components are
   * already rejected by {@link #ensureDeletionSelectorComponents(DeletionSelector)}, before
   * any lexical normalization can hide them; here, the resulting path must have exactly the
   * shape {@code <root>/<organization>/[<prefix>/]<mediaPackage>[/version]}, with the prefix
   * directory being present only if the store root uses one.
   *
   * @param dir
   *          the directory to check
   * @param rootDirectory
   *          the store root the directory is below
   * @param organizationId
   *          the organization identifier
   * @param mediaPackageId
   *          the media package identifier
   * @param version
   *          the version, present if the directory is a version sub-directory
   * @throws AssetStoreException
   *         if the directory does not have the expected shape
   */
  private void ensureMediaPackageShape(File dir, AssetStoreDirectory rootDirectory, String organizationId,
          String mediaPackageId, Optional<Version> version) throws AssetStoreException {
    // The root path is absolute and normalized, and resolving the identifiers does not normalize the result. So an
    // identifier collapsing the path makes it differ from the normalized actual path below.
    Path expected = rootDirectory.mediaPackageDir(organizationId, mediaPackageId);
    if (version.isPresent()) {
      expected = expected.resolve(version.get().toString());
    }
    Path actual = dir.toPath().toAbsolutePath().normalize();
    if (!actual.equals(expected)) {
      throw new AssetStoreException("Deletion target " + actual + " does not point to the media package "
              + mediaPackageId + " (expected " + expected + ")");
    }
  }

  /**
   * Whether the given string is unsafe to use as a single file system path component, i.e. it
   * is the current or parent directory reference or the empty string, or it contains a path
   * separator.
   */
  private static boolean isUnsafePathComponent(String component) {
    return component == null || component.isEmpty() || ".".equals(component) || "..".equals(component)
            || component.indexOf('/') >= 0 || component.indexOf('\\') >= 0;
  }

  /**
   * Rejects deletion selectors whose organization or media package identifiers are unsafe as
   * file system path components (see {@link #isUnsafePathComponent(String)}).
   */
  private void ensureDeletionSelectorComponents(DeletionSelector sel) throws AssetStoreException {
    if (isUnsafePathComponent(sel.getOrganizationId()) || isUnsafePathComponent(sel.getMediaPackageId())) {
      throw new AssetStoreException("Unsafe organization or media package identifier in deletion path: '"
              + sel.getOrganizationId() + "/" + sel.getMediaPackageId() + "'");
    }
  }

  /**
   * Rejects storage paths whose organization, media package or element identifiers are unsafe
   * as file system path components (see {@link #isUnsafePathComponent(String)}), so that the
   * path below the store root cannot collapse into a parent (or sibling) directory.
   */
  private void ensureStoragePathComponents(StoragePath p) throws AssetStoreException {
    if (isUnsafePathComponent(p.getOrganizationId()) || isUnsafePathComponent(p.getMediaPackageId())
            || isUnsafePathComponent(p.getMediaPackageElementId())) {
      throw new AssetStoreException(
              "Unsafe organization, media package or element identifier in storage path: '"
                      + p.getOrganizationId() + "/" + p.getMediaPackageId() + "/" + p.getMediaPackageElementId()
                      + "'");
    }
  }

  /**
   * Returns the directory file from a deletion selector
   *
   * @param rootDirectory
   *          the root directory of the media package the selector refers to
   * @param sel
   *          the deletion selector
   * @return the directory file
   */
  private File getDeletionSelectorDir(AssetStoreDirectory rootDirectory, DeletionSelector sel) {
    final Path mpDir = rootDirectory.mediaPackageDir(sel.getOrganizationId(), sel.getMediaPackageId());
    if (sel.getVersion().isPresent()) {
      return mpDir.resolve(sel.getVersion().get().toString()).toFile();
    }
    return mpDir.toFile();
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
    ensureStoragePathComponents(p);
    AssetStoreDirectory rootDirectory = getRootDirectory(p.getOrganizationId(), p.getMediaPackageId());
    if (rootDirectory == null) {
      rootDirectory = getRootDirectory();
    }
    final File destination = file(rootDirectory, p, extension);
    ensureWithinRoot(destination, rootDirectory.getPath());
    return destination;
  }

  /** Returns a file from a storage path if its media package exists locally, null otherwise */
  private File getExistingFile(StoragePath p, Optional<String> extension) {
    ensureStoragePathComponents(p);
    final AssetStoreDirectory rootDirectory = getRootDirectory(p.getOrganizationId(), p.getMediaPackageId());
    if (rootDirectory == null) {
      return null;
    }
    final File existingFile = file(rootDirectory, p, extension);
    ensureWithinRoot(existingFile, rootDirectory.getPath());
    return existingFile;
  }

  /** The file of a storage path within a root directory */
  private File file(AssetStoreDirectory rootDirectory, StoragePath p, Optional<String> extension) {
    return rootDirectory.mediaPackageDir(p.getOrganizationId(), p.getMediaPackageId())
            .resolve(p.getVersion().toString())
            .resolve(extension.isPresent()
                ? p.getMediaPackageElementId() + EXTENSION_SEPARATOR + extension.get()
                : p.getMediaPackageElementId())
            .toFile();
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
    final File elementFile = getExistingFile(storagePath, Optional.empty());
    if (elementFile == null) {
      // The media package does not exist locally, e.g. because all its snapshots live in a remote asset store
      return Optional.empty();
    }

    var files = elementFile.getParentFile().listFiles(filter);
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
