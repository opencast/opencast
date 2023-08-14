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
public final class DistributionPathUtils {

  private static final Logger logger = LoggerFactory.getLogger(DistributionPathUtils.class);

  /** Key defining the distribution root directory */
  private static final String CONFIG_DOWNLOAD_ROOT = "org.opencastproject.download.directory";

  /** Key defining the distribution URL prefix */
  private static final String CONFIG_DOWNLOAD_URL = "org.opencastproject.download.url";

  private DistributionPathUtils() {
  }

  /**
   * Get the local mount point of the distribution files if it exists.
   *
   * @param componentContext
   *        The OSGI component context
   * @return Path to the local distribution directory if it exists
   */
  public static String getDownloadPath(final ComponentContext componentContext) {
    if (componentContext == null || componentContext.getBundleContext() == null) {
      return null;
    }
    final BundleContext bundleContext = componentContext.getBundleContext();

    String downloadDir = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_DOWNLOAD_ROOT));

    // Is the distribution download available locally?
    if (downloadDir != null && new File(downloadDir).isDirectory()) {
      logger.debug("Found local distribution directory at {}", downloadDir);
      return downloadDir;
    }

    return null;
  }

  /**
   * Get the local mount point of the distribution files if it exists.
   *
   * @param componentContext
   *        The OSGI component context
   * @return Path to the local distribution directory if it exists
   */
  public static String getDownloadUrl(final ComponentContext componentContext) {
    if (componentContext == null || componentContext.getBundleContext() == null) {
      return null;
    }
    final BundleContext bundleContext = componentContext.getBundleContext();

    String downloadUrl = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_DOWNLOAD_URL));

    // Is the distribution download available locally?
    if (downloadUrl != null) {
      logger.debug("Local distribution URL is {}", downloadUrl);
      return downloadUrl;
    }

    return null;
  }


  /**
   * Splits up a distribution URI and returns a local path instead.
   *
   * @param localPath
   *          Path to the local distribution directory
   * @param organizationId
   *          Organization identifier
   * @param uri
   *          URI to the asset
   * @return Local file
   */
  public static File getLocalFile(
      final String localPath,
      final String downloadUrl,
      final String organizationId,
      final URI uri
  ) {
    if (localPath == null
            || organizationId == null
            || !uri.toString().startsWith(downloadUrl)) {
      return null;
    }

    final File file = Paths.get(localPath, uri.toString().substring(downloadUrl.length())).toFile();

    if (file.isFile()) {
      logger.debug("Converted {} to local file at {}", uri, file);
      return file;
    }
    logger.debug("Local file for {} not available. {} does not exist.", uri, file);
    return null;
  }

}
