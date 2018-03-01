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
package org.opencastproject.assetmanager.storage.impl.fs;

import static com.entwinemedia.fn.data.Opt.none;
import static com.entwinemedia.fn.data.Opt.nul;
import static com.entwinemedia.fn.data.Opt.some;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.opencastproject.util.FileSupport.link;
import static org.opencastproject.util.IoSupport.file;
import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.data.functions.Strings.trimToNone;

import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.AssetStoreException;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

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

public abstract class AbstractFileSystemAssetStore implements AssetStore {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractFileSystemAssetStore.class);

  /** The store type e.g. filesystem (short-term), aws (long-term), other implementations */
  protected String storeType = null;

  protected abstract Workspace getWorkspace();

  protected abstract String getRootDirectory();

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
    return findStoragePathFile(from).map(new Fn<File, Boolean>() {
      @Override public Boolean apply(File f) {
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
    }).getOr(false);
  }

  @Override
  public Opt<InputStream> get(final StoragePath path) throws AssetStoreException {
    return findStoragePathFile(path).map(new Fn<File, InputStream>() {
      @Override
      public InputStream apply(File file) {
        try {
          return new FileInputStream(file);
        } catch (FileNotFoundException e) {
          logger.error("Error getting archive file {}", file);
          throw new AssetStoreException(e);
        }
      }
    });
  }

  @Override
  public boolean contains(StoragePath path) throws AssetStoreException {
    return findStoragePathFile(path).isSome();
  }

  @Override
  public boolean delete(DeletionSelector sel) throws AssetStoreException {
    File dir = getDeletionSelectorDir(sel);
    try {
      FileUtils.deleteDirectory(dir);
      // also delete the media package directory if all versions have been deleted
      FileSupport.deleteHierarchyIfEmpty(file(path(getRootDirectory(), sel.getOrganizationId())), dir.getParentFile());
      return true;
    } catch (IOException e) {
      logger.error("Error deleting directory from archive {}", dir);
      throw new AssetStoreException(e);
    }
  }

  /**
   * Returns the directory file from a deletion selector
   *
   * @param sel
   *          the deletion selector
   * @return the directory file
   */
  private File getDeletionSelectorDir(DeletionSelector sel) {
    final String basePath = path(getRootDirectory(), sel.getOrganizationId(), sel.getMediaPackageId());
    for (Version v : sel.getVersion())
      return file(basePath, v.toString());
    return file(basePath);
  }

  /** Create all parent directories of a file. */
  private void mkParent(File f) {
    mkDirs(f.getParentFile());
  }

  /** Create this directory and all of its parents. */
  protected void mkDirs(File d) {
    if (d != null && !d.exists() && !d.mkdirs()) {
      final String msg = "Cannot create directory " + d;
      logger.error(msg);
      throw new AssetStoreException(msg);
    }
  }

  /** Return the extension of a file. */
  private Opt<String> extension(File f) {
    return trimToNone(getExtension(f.getAbsolutePath())).toOpt();
  }

  /** Return the extension of a URI, i.e. the extension of its path. */
  private Opt<String> extension(URI uri) {
    try {
      return trimToNone(getExtension(uri.toURL().getPath())).toOpt();
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
  private File createFile(StoragePath p, Opt<String> extension) {
    return file(
            getRootDirectory(),
            p.getOrganizationId(),
            p.getMediaPackageId(),
            p.getVersion().toString(),
            extension.isSome() ? p.getMediaPackageElementId() + EXTENSION_SEPARATOR + extension.get() : p
                    .getMediaPackageElementId());
  }

  /**
   * Returns a file {@link Option} from a storage path if one is found or an empty {@link Option}
   *
   * @param storagePath
   *          the storage path
   * @return the file {@link Option}
   */
  private Opt<File> findStoragePathFile(final StoragePath storagePath) {
    final FilenameFilter filter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return FilenameUtils.getBaseName(name).equals(storagePath.getMediaPackageElementId());
      }
    };
    final File containerDir = createFile(storagePath, Opt.none(String.class)).getParentFile();
    return nul(containerDir.listFiles(filter)).bind(new Fn<File[], Opt<File>>() {
      @Override
      public Opt<File> apply(File[] files) {
        switch (files.length) {
          case 0:
            return none();
          case 1:
            return some(files[0]);
          default:
            throw new AssetStoreException("Storage path " + files[0].getParent()
                    + "contains multiple files with the same element id!: " + storagePath.getMediaPackageElementId());
        }
      }
    });
  }

  @Override
  public Option<Long> getUsedSpace() {
    return Option.some(FileUtils.sizeOfDirectory(new File(getRootDirectory())));
  }

  @Override
  public Option<Long> getUsableSpace() {
    return Option.some(new File(getRootDirectory()).getUsableSpace());
  }

  @Override
  public Option<Long> getTotalSpace() {
    return Option.some(new File(getRootDirectory()).getTotalSpace());
  }

  @Override
  public String getStoreType() { return storeType; }

}
