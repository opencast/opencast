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

package org.opencastproject.engage.theodul.manager.impl;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A Classloader that loads resources from a bundle or from a directory in the
 * filesystem. The files from the filesystem take precedence over the files from
 * the bundle.
 */
public class StaticResourceClassloader extends ClassLoader {

  private static final Logger logger = LoggerFactory.getLogger(EngagePluginManagerImpl.class);

  private final Bundle bundle;            // Bundle to load resources from
  private final File overrideDir;         // Override directory
  private final String bundlePathPrefix;  // prefix path in bundle resources

  public StaticResourceClassloader(Bundle bundle, File overrideDir, String bundleResourcePath) {
    super();
    this.bundle = bundle;
    this.overrideDir = overrideDir;
    this.bundlePathPrefix = bundleResourcePath;
    logger.info("Bundle={} Override={}", bundle.getSymbolicName(), this.overrideDir.getAbsolutePath());
  }

  @Override
    public URL getResource(String path) {
    // check if override dir is sane. doing this every time so that override
        // directory is optional and can be created during runtime.
    if (overrideIsSane()) {

            // try to find resource in override dir
      String fspath = path.replaceAll("../", "");
      fspath = fspath.replace(bundlePathPrefix, "");
      File file = new File(overrideDir.getAbsoluteFile() + File.separator + fspath);
      if (file.exists() && file.isFile()) {
        try {
          logger.debug("Resource from filesystem overrides bundle resource: {}", file.getAbsolutePath());
          return file.toURI().toURL();
        } catch (MalformedURLException e) {
          logger.error("Failed to get filesystem URL for override file! ", e);
        }
      }
    }

        // if we did not find the resource in filesystem, try to get it from the bundle
    URL out = bundle.getResource(path);
    logger.debug("Serving resource from bundle: {}", out);
    return out;
  }

    /**
     * Tests if the specified filesystem path is an existing readable directory.
     *
     * @return true, iff override path is an existing, readable directory; false
     * otherwise
     */
  public boolean overrideIsSane() {
    return overrideDir.exists() && overrideDir.isDirectory() && overrideDir.canRead();
  }
}
