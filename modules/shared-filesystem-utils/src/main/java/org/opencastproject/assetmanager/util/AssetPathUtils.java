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

package org.opencastproject.assetmanager.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

/**
 *
 */
public final class AssetPathUtils {

  private static final Logger logger = LoggerFactory.getLogger(AssetPathUtils.class);

  /** Key defining the asset manager root directory */
  private static final String CONFIG_ASSET_MANAGER_ROOT = "org.opencastproject.episode.rootdir";

  /** Key defining the main storage directory */
  private static final String CONFIG_STORAGE_DIR = "org.opencastproject.storage.dir";

  private AssetPathUtils() {
  }

  /**
   * Get the local mount point of the asset manager if it exists.
   *
   * @param componentContext
   *        The OSGI component context
   * @return Path to the local asset manager directory if it exists
   */
  public static String getAssetManagerPath(final ComponentContext componentContext) {
    if (componentContext == null || componentContext.getBundleContext() == null) {
      return null;
    }
    final BundleContext bundleContext = componentContext.getBundleContext();

    String assetManagerDir = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_ASSET_MANAGER_ROOT));
    if (assetManagerDir == null) {
      assetManagerDir = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_STORAGE_DIR));
      if (assetManagerDir != null) {
        assetManagerDir = new File(assetManagerDir, "archive").getAbsolutePath();
      }
    }

    // Is the asset manager available locally?
    if (assetManagerDir != null && new File(assetManagerDir).isDirectory()) {
      logger.debug("Found local asset manager directory at {}", assetManagerDir);
      return assetManagerDir;
    }

    return null;
  }

  /**
   * Splits up an asset manager URI and returns a local path instead.
   *
   * @param localPath
   *          Path to the local asset manager directory
   * @param organizationId
   *          Organization identifier
   * @param uri
   *          URI to the asset
   * @return Local file
   */
  public static File getLocalFile(final String localPath, final String organizationId, final URI uri) {
    if (localPath == null
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
    final File file = Paths.get(localPath, organizationId, mediaPackageID, version, filename).toFile();
    if (file.isFile()) {
      logger.debug("Converted {} to local file at {}", uri, file);
      return file;
    }
    logger.debug("Local file for {} not available. {} does not exist.", uri, file);
    return null;
  }

}
