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

package org.opencastproject.assetmanager.util;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.naming.ConfigurationException;

/**
 *
 */
public final class AssetPathUtils {

  private static final Logger logger = LoggerFactory.getLogger(AssetPathUtils.class);

  private AssetPathUtils() {
  }

  /**
   * Get the local mount points of the asset manager if they exist.
   *
   * @param componentContext
   *        The OSGI component context
   * @return The local asset manager directories if the base directory exists, null otherwise
   */
  public static List<AssetStoreDirectory> getAssetManagerPath(final ComponentContext componentContext) {
    if (componentContext == null || componentContext.getBundleContext() == null) {
      return null;
    }

    final List<AssetStoreDirectory> directories;
    try {
      directories = AssetStoreDirectory.fromConfig(componentContext.getBundleContext());
    } catch (IllegalArgumentException e) {
      // The asset store itself refuses to start on invalid configuration. The workspace just falls back to downloads.
      logger.error("Invalid asset manager configuration. Assets will be downloaded instead of being read locally: {}",
          e.getMessage());
      return null;
    }

    // Is the asset manager available locally?
    if (directories == null || !new File(directories.get(0).getPath()).isDirectory()) {
      return null;
    }
    logger.debug("Found local asset manager directory at {}", directories.get(0));

    // Only use directories whose recorded layout matches the configuration. A directory without marker file is
    // accepted since the asset store may not have written it yet.
    final List<AssetStoreDirectory> usableDirectories = new ArrayList<>();
    for (AssetStoreDirectory directory : directories) {
      try {
        final Optional<Integer> recorded = directory.readMarker();
        if (recorded.isPresent() && recorded.get() != directory.getPrefixLength()) {
          logger.error("Asset manager directory {} was created with a prefix length of {} but {} is configured. "
              + "Assets in this directory will be downloaded instead of being read locally.",
              directory.getPath(), recorded.get(), directory.getPrefixLength());
          continue;
        }
      } catch (ConfigurationException | IOException e) {
        logger.error("Unable to read the layout of asset manager directory {}. "
            + "Assets in this directory will be downloaded instead of being read locally.", directory.getPath(), e);
        continue;
      }
      usableDirectories.add(directory);
    }
    return usableDirectories;
  }

  /**
   * Splits up an asset manager URI and returns a local path instead.
   *
   * @param localPaths
   *          Potential local asset manager directories
   * @param organizationId
   *          Organization identifier
   * @param uri
   *          URI to the asset
   * @return Local file
   */
  public static File getLocalFile(final List<AssetStoreDirectory> localPaths, final String organizationId,
          final URI uri) {
    if (localPaths == null
            || organizationId == null
            || !uri.getScheme().startsWith("http")
            || !uri.getPath().startsWith("/assets/assets/")) {
      return null;
    }

    final String[] assetPath = uri.getPath().split("/");
    // /assets/assets/{mediaPackageID}/{mediaPackageElementID}/{version}/{filenameIgnore}
    if (assetPath.length != 7) {
      return null;
    }

    final String mediaPackageID = assetPath[3];
    final String mediaPackageElementID = assetPath[4];
    final String version = assetPath[5];
    final String filename = mediaPackageElementID + '.' + FilenameUtils.getExtension(assetPath[6]);
    // Check under which path the mediapackage exists. Like the asset store, use the first match.
    Path mediaPackageDir = null;
    for (AssetStoreDirectory directory : localPaths) {
      Path dirPath = directory.mediaPackageDir(organizationId, mediaPackageID);
      if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
        mediaPackageDir = dirPath;
        break;
      }
    }
    // if remote asset stores are used, mediaPackageDir could be null (i.e. all snapshots live remotely)
    if (mediaPackageDir == null) {
      logger.debug("Local snapshot for {} not available.", uri);
      return null;
    }
    final File file = mediaPackageDir.resolve(version).resolve(filename).toFile();
    if (file.isFile()) {
      logger.debug("Converted {} to local file at {}", uri, file);
      return file;
    }
    logger.debug("Local file for {} not available. {} does not exist.", uri, file);
    return null;
  }

}
