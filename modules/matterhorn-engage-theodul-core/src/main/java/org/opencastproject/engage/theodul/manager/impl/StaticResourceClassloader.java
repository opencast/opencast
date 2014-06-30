/**
 * Copyright 2009, 2010 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.engage.theodul.manager.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Classloader that loads resources from a bundle or from a directory in the
 * filesystem. The files from the filesystem take precedence over the files from
 * the bundle.
 */
public class StaticResourceClassloader extends ClassLoader {

  private static final Logger logger = LoggerFactory.getLogger(EngagePluginManagerImpl.class);

  private final Bundle bundle;    // Bundle to load resources from
  private final File odr;         // Override directory
  private final String bpp;       // prefix path in bundle resources

  public StaticResourceClassloader(Bundle bundle, File overrideDir, String bundleResourcePath) {
    super();
    this.bundle = bundle;
    this.odr = overrideDir;
    this.bpp = bundleResourcePath;
    logger.info("Bundle={} Override={}", bundle.getSymbolicName(), odr.getAbsolutePath());
  }

  @Override
  public URL getResource(String path) {
    // check if override dir is sane. doing this every time so that override
    // directory is optional and can be created during runtime.
    if (overrideIsSane()) {
      
      // try to find resource in override dir
      String fspath = path.replace(bpp+"/", "");
      File file = new File(odr.getAbsoluteFile() + File.separator + fspath);
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
    logger.debug("Serving resource from bundle: {}", path);
    return bundle.getResource(path);
  }
  
  /** Tests if the specified filesystem path is an existing readable directory.
   * 
   * @return true, iff override path is an existing, readable directory; false otherwise
   */
  public boolean overrideIsSane() {
    return odr.exists() && odr.isDirectory() && odr.canRead();
  }
}
