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

import static org.opencastproject.util.IoSupport.file;

import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
  private LoadingCache<String, Object> cache = null;
  private int cacheSize = 1000;
  private int cacheExpiration = 60;
  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** Configuration key for the default Opencast storage directory. A value is optional. */
  public static final String CFG_OPT_STORAGE_DIR = "org.opencastproject.storage.dir";

  /**
   * The default store directory name.
   * Will be used in conjunction with {@link #CFG_OPT_STORAGE_DIR} if {@link #CFG_OPT_STORAGE_DIR} is not set.
   */
  private static final String DEFAULT_STORE_DIRECTORY = "archive";

  /** Configuration key for the archive root directory. */
  public static final String CONFIG_STORE_ROOT_DIR = "org.opencastproject.episode.rootdir";

  /** The root directories for storing files (typically one) */
  private List<String> rootDirectories;

  /** The workspace */
  private Workspace workspace;

  @Override protected Workspace getWorkspace() {
    return workspace;
  }

  @Override
  protected String getRootDirectory() {
    // Determine which storage to return by amount of remaining usable space
    long usableSpace = 0;
    String mostUsableDirectory = null;
    for (String path : rootDirectories) {
      Option<Long> maybeUsableSpace = Option.some(new File(path).getUsableSpace());
      if (maybeUsableSpace.isNone()) {
        continue;
      }
      if (maybeUsableSpace.get() > usableSpace) {
        usableSpace = maybeUsableSpace.get();
        mostUsableDirectory = path;
      }
    }

    return mostUsableDirectory;
  }

  /**
   * Looks for the root directory of the given mediapackage id
   * @param orgId the organization which the mediapackage belongs to
   * @param mpId the mediapackage id
   * @return The root directory path of the given mediapackage, or null if the mediapackage could not be found anywhere
   */
  protected String getRootDirectory(String orgId, String mpId) {
    try {
      Object path = cache.getUnchecked(Paths.get(orgId, mpId).toString());
      if (path == nullToken) {
        logger.debug("Path could not be found, returning null.");
        return null;
      } else {
        logger.debug("Returning path to mediapackage " + mpId);
        return (String) path;
      }
    } catch (ExecutionError e) {
      logger.warn("Exception while getting path for mediapackage {}", mpId, e);
      return null;
    } catch (UncheckedExecutionException e) {
      logger.warn("Exception while getting path for  mediapackage {}", mpId, e);
      return null;
    }
  }

  private String getRootDirectoryForMediaPackage(String orgAndMpId) {
    // Search the mediapackage on all storages
    for (String path : rootDirectories) {
      Path dirPath = Path.of(path, orgAndMpId);
      if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
        return path;
      }
    }

    return null;
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
  public void activate(final ComponentContext cc)
          throws IllegalStateException, IOException {
    storeType = (String) cc.getProperties().get(AssetStore.STORE_TYPE_PROPERTY);
    logger.info("{} is: {}", AssetStore.STORE_TYPE_PROPERTY, storeType);

    // Read in multiple directories
    rootDirectories = new ArrayList<>();
    int index = 1;
    boolean isRootDirectory = true;
    while (isRootDirectory) {
      String directory;
      try {
        directory = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_STORE_ROOT_DIR + "." + index));
      } catch (Exception e) {
        isRootDirectory = false;
        throw e;
      }

      if (directory != null) {
        rootDirectories.add(directory);
      } else {
        isRootDirectory = false;
      }
      index++;
    }
    for (String directory: rootDirectories) {
      mkDirs(file(directory));
    }

    // Read in single directory
    if (rootDirectories.size() == 0) {
      String rootDirectory = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_STORE_ROOT_DIR));
      if (rootDirectory == null) {
        final String storageDir = StringUtils.trimToNull(cc.getBundleContext().getProperty(CFG_OPT_STORAGE_DIR));
        if (storageDir == null) {
          throw new IllegalArgumentException("Storage directory must be set");
        }
        rootDirectory = Paths.get(storageDir, DEFAULT_STORE_DIRECTORY).toFile().getAbsolutePath();
      }
      mkDirs(file(rootDirectory));
      rootDirectories.add(rootDirectory);
    }

    logger.info("Start asset manager files system store at {}", rootDirectories);

    // Setup rootDirectory cache
    // Remembers the rootdirectory for a given mediapackage
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String orgAndMpId) throws Exception {
                String rootDirectory = getRootDirectoryForMediaPackage(orgAndMpId);
                return rootDirectory == null ? null : rootDirectory;
              }
            });
  }
}
