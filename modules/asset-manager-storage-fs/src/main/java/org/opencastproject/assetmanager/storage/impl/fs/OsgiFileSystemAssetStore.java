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

import static org.opencastproject.util.IoSupport.file;

import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.util.AssetStoreDirectory;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.io.FileUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

@Component(
    property = {
    "service.description=File system based asset store",
    "store.type=local-filesystem"
    },
    immediate = true,
    service = { AssetStore.class }
)
public class OsgiFileSystemAssetStore extends AbstractFileSystemAssetStore {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(OsgiFileSystemAssetStore.class);

  /** A cache of mediapckage ids and their associated storages */
  private LoadingCache<String, Optional<AssetStoreDirectory>> cache = null;
  private int cacheSize = 1000;
  private int cacheExpiration = 1;

  /** Configuration key for the default Opencast storage directory. A value is optional. */
  public static final String CFG_OPT_STORAGE_DIR = AssetStoreDirectory.CONFIG_STORAGE_DIR;

  /** Configuration key for the archive root directory. */
  public static final String CONFIG_STORE_ROOT_DIR = AssetStoreDirectory.CONFIG_ROOT_DIR;

  /** The root directories for storing files (typically one) */
  private List<AssetStoreDirectory> rootDirectories;

  /** The workspace */
  private Workspace workspace;

  @Override protected Workspace getWorkspace() {
    return workspace;
  }

  @Override
  /**
   * Returns the root directory with the most usable space left
   * @return The root directory
   */
  protected AssetStoreDirectory getRootDirectory() {
    // Determine which storage to return by amount of remaining usable space
    long usableSpace = 0;
    AssetStoreDirectory mostUsableDirectory = null;
    for (AssetStoreDirectory directory : rootDirectories) {
      Optional<Long> maybeUsableSpace = Optional.of(new File(directory.getPath()).getUsableSpace());
      if (maybeUsableSpace.isEmpty()) {
        continue;
      }
      if (maybeUsableSpace.get() > usableSpace) {
        usableSpace = maybeUsableSpace.get();
        mostUsableDirectory = directory;
      }
    }

    return mostUsableDirectory;
  }

  /**
   * Looks for the root directory of the given mediapackage id
   * @param orgId the organization which the mediapackage belongs to
   * @param mpId the mediapackage id
   * @return The root directory of the given mediapackage, or null if the mediapackage could not be found anywhere
   */
  protected AssetStoreDirectory getRootDirectory(String orgId, String mpId) {
    try {
      String cacheKey = Paths.get(orgId, mpId).toString();
      Optional<AssetStoreDirectory> directory = cache.getUnchecked(cacheKey);
      if (directory.isPresent()) {
        logger.debug("Root directory for mediapackage {} is {}", mpId, directory.get());
        return directory.get();
      } else {
        logger.debug("Root directory for mediapackage {} could not be found, returning null.", mpId);
        cache.invalidate(cacheKey);
        return null;
      }
    } catch (ExecutionError e) {
      logger.warn("Exception while getting path for mediapackage {}", mpId, e);
      return null;
    } catch (UncheckedExecutionException e) {
      logger.warn("Exception while getting path for  mediapackage {}", mpId, e);
      return null;
    }
  }

  /**
   * Looks for the root directory that contains the given mediapackage id.
   * Used by the cache.
   * @param orgId the organization which the mediapackage belongs to
   * @param mpId the mediapackage id
   * @return The root directory of the given mediapackage
   */
  private AssetStoreDirectory getRootDirectoryForMediaPackage(String orgId, String mpId) {
    // Search the mediapackage on all storages, each with its own directory layout
    for (AssetStoreDirectory directory : rootDirectories) {
      Path dirPath = directory.mediaPackageDir(orgId, mpId);
      if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
        return directory;
      }
    }

    return null;
  }

  private List<AssetStoreDirectory> getRootDirectories() {
    return Collections.unmodifiableList(rootDirectories);
  }

  protected void setupCache() {
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<AssetStoreDirectory>>() {
              @Override
              public Optional<AssetStoreDirectory> load(String orgAndMpId) throws Exception {
                Path key = Paths.get(orgAndMpId);
                return Optional.ofNullable(
                    getRootDirectoryForMediaPackage(key.getName(0).toString(), key.getName(1).toString()));
              }
            });
  }

  protected void onDeleteMediaPackage(String orgId, String mpId) {
    String cacheKey = Paths.get(orgId, mpId).toString();
    cache.invalidate(cacheKey);
  }

  /**
   * OSGi DI.
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  @Activate
  public void activate(final ComponentContext cc) throws IllegalStateException, IOException, ConfigurationException {
    storeType = (String) cc.getProperties().get(AssetStore.STORE_TYPE_PROPERTY);
    logger.info("{} is: {}", AssetStore.STORE_TYPE_PROPERTY, storeType);

    // Read in the directories and their layout
    rootDirectories = AssetStoreDirectory.fromConfig(cc.getBundleContext());
    if (rootDirectories == null) {
      throw new IllegalArgumentException("Storage directory must be set");
    }

    // Check for bad configuration
    for (int i = 0; i < rootDirectories.size(); i++) {
      for (int j = 0; j < rootDirectories.size(); j++) {
        if (i == j) {
          continue;
        }
        if (isChild(rootDirectories.get(j).getPath(), rootDirectories.get(i).getPath())) {
          throw new ConfigurationException("Storage directory " + rootDirectories.get(j) + " is a subdirectory of "
              + rootDirectories.get(i) + ". This is not allowed.");
        }
      }
    }
    // Create
    for (AssetStoreDirectory directory : rootDirectories) {
      mkDirs(file(directory.getPath()));
    }
    // Check for write access
    for (AssetStoreDirectory directory : rootDirectories) {
      File tmp = new File(directory.getPath() + "/tobedeleted.tmp");
      tmp.createNewFile();
      tmp.delete();
    }
    // Make sure the configured layout matches the data already in the directories
    for (AssetStoreDirectory directory : rootDirectories) {
      directory.verifyOrWriteMarker();
    }

    logger.info("Start asset manager files system store at {}", rootDirectories);

    // Setup rootDirectory cache
    // Remembers the root directory for a given mediapackage
    setupCache();
  }

  private static boolean isChild(String childText, String parentText) {
    Path parent = Paths.get(parentText).toAbsolutePath();
    Path child = Paths.get(childText).toAbsolutePath();
    if (child.startsWith(parent)) {
      return true;
    }
    return false;
  }

  // Depending on how these functions are used, it may not make sense to just sum over all root directories.
  // It would likely be more proper to return the individual values for each directory in a collection.
  // However, that would require a major rewrite of the StorageUsage interface, which is a lot of work for some
  // functions that seem to see no use anyhow.
  @Override
  public Optional<Long> getUsedSpace() {
    long usedSpace = 0;
    for (AssetStoreDirectory directory : rootDirectories) {
      usedSpace += FileUtils.sizeOfDirectory(new File(directory.getPath()));
    }
    return Optional.of(usedSpace);
  }

  @Override
  public Optional<Long> getUsableSpace() {
    long usableSpace = 0;
    for (AssetStoreDirectory directory : rootDirectories) {
      usableSpace += new File(directory.getPath()).getUsableSpace();
    }
    return Optional.of(usableSpace);
  }

  @Override
  public Optional<Long> getTotalSpace() {
    long totalSpace = 0;
    for (AssetStoreDirectory directory : rootDirectories) {
      totalSpace += new File(directory.getPath()).getTotalSpace();
    }
    return Optional.of(totalSpace);
  }

}
