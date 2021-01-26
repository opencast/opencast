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


package org.opencastproject.adminui.index;

import org.opencastproject.util.PathSupport;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Utilities to ease dealing with Elasticsearch.
 */
final class AdminUIElasticsearchUtils {

  /**
   * Private constructor to make sure this class is used as a utility class.
   */
  private AdminUIElasticsearchUtils() {
  }

  /**
   * Creates an elastic search index configuration inside the given directory by loading the relevant configuration
   * files from the bundle.
   *
   * @param homeDirectory
   *          the configuration directory
   * @param index
   *          the index name
   * @throws IOException
   *           if creating the configuration fails
   */
  static void createIndexConfigurationAt(File homeDirectory, String index) throws IOException {

    // Load the index configuration and move it into place
    File configurationRoot = new File(PathSupport.concat(new String[] { homeDirectory.getAbsolutePath(), "etc",
            "index", index }));
    FileUtils.deleteQuietly(configurationRoot);
    if (!configurationRoot.mkdirs()) {
      throw new IOException("Error creating " + configurationRoot);
    }

    String[] files = new String[] { "default-mapping.json", "event-mapping.json", "group-mapping.json",
            "series-mapping.json", "settings.yml", "theme-mapping.json", "version-mapping.json" };

    for (String file : files) {
      String bundleLocation = Paths.get("/index", index, file).toString();
      File fileLocation = new File(configurationRoot, file);
      FileUtils.copyInputStreamToFile(
              AdminUIElasticsearchUtils.class.getResourceAsStream(bundleLocation),
              fileLocation);
    }
  }

}
